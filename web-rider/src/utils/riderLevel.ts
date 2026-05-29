const LEVEL_META: Record<string, { label: string; color: string; nextScore: number | null }> = {
  BRONZE: { label: '青铜骑手', color: '#cd7f32', nextScore: 100 },
  SILVER: { label: '白银骑手', color: '#c0c0c0', nextScore: 300 },
  GOLD: { label: '黄金骑手', color: '#ffd700', nextScore: 600 },
  DIAMOND: { label: '钻石骑手', color: '#40c9ff', nextScore: null }
}

const LEGACY_LEVEL_MAP: Record<number, string> = {
  1: 'BRONZE',
  2: 'SILVER',
  3: 'GOLD',
  4: 'DIAMOND'
}

export function normalizeRiderLevel(level: unknown): string {
  if (typeof level === 'string' && LEVEL_META[level]) return level
  if (typeof level === 'number' && LEGACY_LEVEL_MAP[level]) return LEGACY_LEVEL_MAP[level]
  return 'BRONZE'
}

export function getRiderLevelLabel(level: unknown): string {
  return LEVEL_META[normalizeRiderLevel(level)].label
}

export function getRiderLevelColor(level: unknown): string {
  return LEVEL_META[normalizeRiderLevel(level)].color
}

export function getRiderNextLevelScore(level: unknown): number | null {
  return LEVEL_META[normalizeRiderLevel(level)].nextScore
}
