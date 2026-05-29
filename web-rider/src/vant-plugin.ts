import type { App } from 'vue'
import {
  NavBar, Tabbar, TabbarItem, Tab, Tabs,
  Form, Field, CellGroup, Cell,
  Button, Switch, Stepper, Rate,
  Image as VanImage, Icon,
  Tag, Badge,
  PullRefresh, List, Empty,
  Popup, ActionSheet,
  Checkbox, CheckboxGroup,
  Radio, RadioGroup,
  Steps, Step,
  Grid, GridItem,
  Divider, Loading,
  NoticeBar, Search,
  Skeleton, Card, SubmitBar,
  Uploader,
  Swipe, SwipeItem,
  DropdownMenu, DropdownItem,
  Overlay, Dialog,
} from 'vant'

const comps = [
  NavBar, Tabbar, TabbarItem, Tab, Tabs,
  Form, Field, CellGroup, Cell,
  Button, Switch, Stepper, Rate,
  VanImage, Icon,
  Tag, Badge,
  PullRefresh, List, Empty,
  Popup, ActionSheet,
  Checkbox, CheckboxGroup,
  Radio, RadioGroup,
  Steps, Step,
  Grid, GridItem,
  Divider, Loading,
  NoticeBar, Search,
  Skeleton, Card, SubmitBar,
  Uploader,
  Swipe, SwipeItem,
  DropdownMenu, DropdownItem,
  Overlay, Dialog,
]

export function setupVant(app: App) {
  comps.forEach(c => {
    if (c && c.name) app.component(c.name, c)
  })
}
