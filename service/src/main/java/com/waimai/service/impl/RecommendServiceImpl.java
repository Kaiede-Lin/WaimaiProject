package com.waimai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.waimai.common.entity.*;
import com.waimai.service.mapper.*;
import com.waimai.service.service.RecommendService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class RecommendServiceImpl implements RecommendService {

    private static final Logger log = LoggerFactory.getLogger(RecommendServiceImpl.class);

    private final OrderMapper orderMapper;
    private final OrderDetailMapper orderDetailMapper;
    private final DishMapper dishMapper;
    private final CategoryMapper categoryMapper;
    private final MerchantMapper merchantMapper;

    @Value("${app.ai.deepseek.api-key}")
    private String apiKey;

    @Value("${app.ai.deepseek.base-url}")
    private String baseUrl;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public RecommendServiceImpl(OrderMapper orderMapper, OrderDetailMapper orderDetailMapper,
                                DishMapper dishMapper, CategoryMapper categoryMapper,
                                MerchantMapper merchantMapper) {
        this.orderMapper = orderMapper;
        this.orderDetailMapper = orderDetailMapper;
        this.dishMapper = dishMapper;
        this.categoryMapper = categoryMapper;
        this.merchantMapper = merchantMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Map<String, Object> chat(Long userId, String message) {
        String msg = message != null ? message.trim() : "";
        if (msg.isEmpty()) {
            return localChat(userId, msg);
        }

        try {
            Map<String, Object> aiResult = deepseekChat(userId, msg);
            if (aiResult != null && aiResult.containsKey("dishes")) {
                return aiResult;
            }
        } catch (Exception e) {
            log.warn("DeepSeek API call failed, falling back to local logic: {}", e.getMessage());
        }

        return localChat(userId, msg);
    }

    // ─── DeepSeek AI ─────────────────────────────────────────────────

    private Map<String, Object> deepseekChat(Long userId, String message) throws Exception {
        String menuContext = buildMenuContext();
        String systemPrompt = buildSystemPrompt(menuContext);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-chat");
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 1024);
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", message)
        ));

        String json = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("DeepSeek API returned status " + response.statusCode() + ": " + response.body());
        }

        Map<String, Object> respBody = objectMapper.readValue(response.body(), new TypeReference<>() {});

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> choices = (List<Map<String, Object>>) respBody.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("DeepSeek returned empty choices");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
        String content = (String) msg.get("content");
        log.info("DeepSeek raw response: {}", content);

        return parseAiResponse(content);
    }

    private String buildMenuContext() {
        List<Dish> dishes = dishMapper.selectList(new LambdaQueryWrapper<Dish>()
                .eq(Dish::getStatus, 1)
                .orderByDesc(Dish::getMonthlySales)
                .last("LIMIT 80"));

        if (dishes.isEmpty()) return "暂无菜品";

        Map<Long, String> catNames = new HashMap<>();
        Map<Long, String> mchNames = new HashMap<>();

        for (Dish d : dishes) {
            if (!catNames.containsKey(d.getCategoryId())) {
                Category cat = categoryMapper.selectById(d.getCategoryId());
                catNames.put(d.getCategoryId(), cat != null ? cat.getName() : "未分类");
            }
            if (!mchNames.containsKey(d.getMerchantId())) {
                Merchant mch = merchantMapper.selectById(d.getMerchantId());
                mchNames.put(d.getMerchantId(), mch != null ? mch.getName() : "未知商家");
            }
        }

        StringBuilder sb = new StringBuilder();
        for (Dish d : dishes) {
            sb.append(String.format("ID:%d | %s | ¥%.1f | 月销%d | %s | %s\n",
                    d.getId(), d.getName(), d.getPrice(),
                    d.getMonthlySales() != null ? d.getMonthlySales() : 0,
                    catNames.getOrDefault(d.getCategoryId(), ""),
                    mchNames.getOrDefault(d.getMerchantId(), "")));
        }
        return sb.toString();
    }

    private String buildSystemPrompt(String menu) {
        return """
                你是外卖平台的美食推荐助手。根据用户的口味偏好、预算等需求，从当前可点菜品中推荐最合适的。

                当前可点菜品（格式：ID | 菜名 | 价格 | 月销量 | 分类 | 商家）：
                %s

                请严格按以下JSON格式回复（不要包含其他文字，不要用markdown代码块包裹）：
                {"reply":"用1-2句话向用户说明推荐理由，语气亲切自然","dishIds":[1,3,5]}

                dishIds数组包含3-5道推荐菜品的ID，按推荐优先级排序。只推荐菜单中真实存在的菜品ID。
                """.formatted(menu);
    }

    private Map<String, Object> parseAiResponse(String content) throws Exception {
        // Strip markdown code fences if present
        String json = content.trim();
        if (json.startsWith("```")) {
            json = json.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
        }

        Map<String, Object> aiResult = objectMapper.readValue(json, new TypeReference<>() {});

        String reply = (String) aiResult.getOrDefault("reply", "为你推荐以下菜品：");

        @SuppressWarnings("unchecked")
        List<Integer> dishIdsRaw = (List<Integer>) aiResult.get("dishIds");
        List<Long> dishIds = new ArrayList<>();
        if (dishIdsRaw != null) {
            for (Number n : dishIdsRaw) {
                dishIds.add(n.longValue());
            }
        }

        // Look up dishes from DB
        List<Map<String, Object>> dishes = new ArrayList<>();
        for (Long dishId : dishIds) {
            Dish d = dishMapper.selectById(dishId);
            if (d != null && d.getStatus() != null && d.getStatus() == 1) {
                dishes.add(toDishMap(d));
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("reply", reply);
        result.put("dishes", dishes);
        return result;
    }

    // ─── Local fallback (rule-based) ─────────────────────────────────

    private Map<String, Object> localChat(Long userId, String message) {
        String msg = message != null ? message.trim() : "";
        String intent = detectIntent(msg);

        return switch (intent) {
            case "hot"        -> hotRanking();
            case "budget"     -> budgetFriendly();
            case "spicy"      -> tasteFilter("辣|麻辣|香辣|酸辣");
            case "sweet"      -> tasteFilter("甜|糖|甜品|奶茶");
            case "light"      -> tasteFilter("清淡|粥|汤|蒸|素");
            case "breakfast"  -> mealTime("早餐|粥|面|豆浆|包子|油条|鸡蛋");
            case "lunch"      -> mealTime("套餐|饭|菜|煲|锅");
            case "dinner"     -> mealTime("火锅|烧烤|烤鱼|煲|锅|大餐");
            case "snack"      -> mealTime("小吃|炸鸡|烧烤|奶茶|甜品|卤味");
            case "combo"      -> mealCombo(userId);
            default           -> personalized(userId);
        };
    }

    // ─── Intent detection ─────────────────────────────────────────

    private String detectIntent(String msg) {
        if (msg.contains("辣")) return "spicy";
        if (msg.contains("甜") || msg.contains("奶茶") || msg.contains("甜品")) return "sweet";
        if (msg.contains("清淡") || msg.contains("粥") || msg.contains("素")) return "light";
        if (msg.contains("早餐") || msg.contains("早上")) return "breakfast";
        if (msg.contains("午餐") || msg.contains("中午")) return "lunch";
        if (msg.contains("晚餐") || msg.contains("晚上")) return "dinner";
        if (msg.contains("夜宵") || msg.contains("宵夜")) return "snack";
        if (msg.contains("便宜") || msg.contains("实惠") || msg.contains("省钱")) return "budget";
        if (msg.contains("热销") || msg.contains("排行") || msg.contains("火爆") || msg.contains("最火")) return "hot";
        if (msg.contains("搭配") || msg.contains("组合") || msg.contains("一餐")) return "combo";
        return "personalized";
    }

    // ─── Core recommendation engines ──────────────────────────────

    private Map<String, Object> personalized(Long userId) {
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreateTime)
                .last("LIMIT 50"));

        Set<Long> triedDishIds = new HashSet<>();
        Map<Long, Integer> categoryCount = new HashMap<>();
        double totalSpend = 0;
        int orderCount = 0;

        for (Order o : orders) {
            if ("CANCELLED".equals(o.getStatus())) continue;
            orderCount++;
            totalSpend += o.getPayAmount() != null ? o.getPayAmount().doubleValue() : 0;
            List<OrderDetail> details = orderDetailMapper.selectList(
                    new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, o.getId()));
            for (OrderDetail d : details) {
                triedDishIds.add(d.getDishId());
                Dish dish = dishMapper.selectById(d.getDishId());
                if (dish != null) {
                    categoryCount.merge(dish.getCategoryId(), d.getQuantity(), Integer::sum);
                }
            }
        }

        List<Long> favCategories = categoryCount.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(2)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        List<Map<String, Object>> dishes = new ArrayList<>();
        for (Long catId : favCategories) {
            List<Dish> catDishes = dishMapper.selectList(new LambdaQueryWrapper<Dish>()
                    .eq(Dish::getCategoryId, catId)
                    .eq(Dish::getStatus, 1)
                    .orderByDesc(Dish::getMonthlySales)
                    .last("LIMIT 5"));
            for (Dish d : catDishes) {
                if (!triedDishIds.contains(d.getId())) {
                    dishes.add(toDishMap(d));
                }
            }
        }

        if (dishes.isEmpty()) {
            return hotRanking();
        }

        Category topCat = favCategories.isEmpty() ? null : categoryMapper.selectById(favCategories.get(0));
        StringBuilder reply = new StringBuilder();
        reply.append("根据你的口味偏好");
        if (topCat != null) reply.append("（最爱「").append(topCat.getName()).append("」）");
        reply.append("，为你推荐 ").append(dishes.size()).append(" 道没尝过的菜品～");

        Map<String, Object> result = new HashMap<>();
        result.put("reply", reply.toString());
        result.put("dishes", dishes);
        return result;
    }

    private Map<String, Object> hotRanking() {
        List<Dish> hot = dishMapper.selectList(new LambdaQueryWrapper<Dish>()
                .eq(Dish::getStatus, 1)
                .orderByDesc(Dish::getMonthlySales)
                .last("LIMIT 10"));

        List<Map<String, Object>> dishes = hot.stream().map(this::toDishMap).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("reply", "为你整理近期热销排行 Top" + dishes.size() + "，大家都在点这些：");
        result.put("dishes", dishes);
        return result;
    }

    private Map<String, Object> budgetFriendly() {
        List<Dish> cheap = dishMapper.selectList(new LambdaQueryWrapper<Dish>()
                .eq(Dish::getStatus, 1)
                .lt(Dish::getPrice, new BigDecimal("15"))
                .orderByDesc(Dish::getMonthlySales)
                .last("LIMIT 10"));

        List<Map<String, Object>> dishes = cheap.stream().map(this::toDishMap).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("reply", "为你精选 " + dishes.size() + " 道实惠好菜，人均不到¥15：");
        result.put("dishes", dishes);
        return result;
    }

    private Map<String, Object> tasteFilter(String regex) {
        List<Dish> all = dishMapper.selectList(new LambdaQueryWrapper<Dish>()
                .eq(Dish::getStatus, 1)
                .orderByDesc(Dish::getMonthlySales)
                .last("LIMIT 200"));

        Pattern p = Pattern.compile(regex);
        List<Dish> matched = all.stream()
                .filter(d -> (d.getName() != null && p.matcher(d.getName()).find())
                        || (d.getRichDescription() != null && p.matcher(d.getRichDescription()).find())
                        || (d.getSummary() != null && p.matcher(d.getSummary()).find()))
                .limit(10)
                .collect(Collectors.toList());

        List<Map<String, Object>> dishes = matched.stream().map(this::toDishMap).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("reply", "为你找到 " + dishes.size() + " 道匹配口味的菜品：");
        result.put("dishes", dishes);
        return result;
    }

    private Map<String, Object> mealTime(String regex) {
        List<Dish> all = dishMapper.selectList(new LambdaQueryWrapper<Dish>()
                .eq(Dish::getStatus, 1)
                .orderByDesc(Dish::getMonthlySales)
                .last("LIMIT 200"));

        Pattern p = Pattern.compile(regex);
        Set<Long> matchedCatIds = new HashSet<>();
        List<Category> allCats = categoryMapper.selectList(null);
        for (Category c : allCats) {
            if (c.getName() != null && p.matcher(c.getName()).find()) {
                matchedCatIds.add(c.getId());
            }
        }

        List<Dish> matched = all.stream()
                .filter(d -> matchedCatIds.contains(d.getCategoryId())
                        || (d.getName() != null && p.matcher(d.getName()).find())
                        || (d.getRichDescription() != null && p.matcher(d.getRichDescription()).find()))
                .limit(10)
                .collect(Collectors.toList());

        List<Map<String, Object>> dishes = matched.stream().map(this::toDishMap).collect(Collectors.toList());

        int hour = LocalTime.now().getHour();
        String timeName = hour < 10 ? "早餐" : hour < 14 ? "午餐" : hour < 21 ? "晚餐" : "夜宵";

        Map<String, Object> result = new HashMap<>();
        result.put("reply", "这个时间适合来份" + timeName + "，为你推荐 " + dishes.size() + " 道：");
        result.put("dishes", dishes);
        return result;
    }

    private Map<String, Object> mealCombo(Long userId) {
        List<Dish> all = dishMapper.selectList(new LambdaQueryWrapper<Dish>()
                .eq(Dish::getStatus, 1)
                .orderByDesc(Dish::getMonthlySales)
                .last("LIMIT 200"));

        List<Dish> staples = filterByCategory(all, "饭|面|粉|粥|饺子|米线");
        List<Dish> mains = filterByCategory(all, "菜|煲|锅|肉|鱼|鸡|牛|虾");
        List<Dish> drinks = filterByCategory(all, "奶茶|饮品|饮料|咖啡|果汁|茶");

        Random rng = new Random();
        List<Map<String, Object>> combo = new ArrayList<>();
        if (!staples.isEmpty()) combo.add(toDishMap(staples.get(rng.nextInt(staples.size()))));
        if (!mains.isEmpty()) combo.add(toDishMap(mains.get(rng.nextInt(mains.size()))));
        if (!drinks.isEmpty()) combo.add(toDishMap(drinks.get(rng.nextInt(drinks.size()))));

        double total = combo.stream().mapToDouble(d -> ((Number) d.get("price")).doubleValue()).sum();

        Map<String, Object> result = new HashMap<>();
        result.put("reply", "为你搭配一餐：主食+主菜+饮品，总价约 ¥" + String.format("%.1f", total) + "，营养均衡～");
        result.put("dishes", combo);
        return result;
    }

    // ─── Helpers ───────────────────────────────────────────────────

    private List<Dish> filterByCategory(List<Dish> dishes, String regex) {
        Pattern p = Pattern.compile(regex);
        Set<Long> catIds = new HashSet<>();
        for (Category c : categoryMapper.selectList(null)) {
            if (c.getName() != null && p.matcher(c.getName()).find()) {
                catIds.add(c.getId());
            }
        }
        return dishes.stream().filter(d -> catIds.contains(d.getCategoryId())).collect(Collectors.toList());
    }

    private Map<String, Object> toDishMap(Dish d) {
        Map<String, Object> m = new HashMap<>();
        m.put("dishId", d.getId());
        m.put("name", d.getName());
        m.put("image", d.getImage());
        m.put("price", d.getPrice());
        m.put("originalPrice", d.getOriginalPrice());
        m.put("monthlySales", d.getMonthlySales());
        m.put("summary", d.getSummary());
        m.put("merchantId", d.getMerchantId());

        Merchant merchant = merchantMapper.selectById(d.getMerchantId());
        m.put("merchantName", merchant != null ? merchant.getName() : "未知商家");

        Category cat = categoryMapper.selectById(d.getCategoryId());
        m.put("categoryName", cat != null ? cat.getName() : "");

        return m;
    }
}
