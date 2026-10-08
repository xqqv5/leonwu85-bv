package dev.aaa1115910.bv.mobile.settings

enum class MobileBottomNavItem(val displayName: String) {
    Home("首页"),
    Dynamic("动态"),
    History("历史"),
    Favorite("收藏"),
    Mine("我的"),
    Setting("设置");

    companion object {
        val defaultItems: List<MobileBottomNavItem> = listOf(Home, Dynamic, Setting)

        fun sanitize(items: Collection<MobileBottomNavItem>): List<MobileBottomNavItem> =
            entries.filter { it in items }.ifEmpty { defaultItems }

        fun fromPreference(value: String): List<MobileBottomNavItem> = sanitize(
            value.split(',').mapNotNull { name -> entries.firstOrNull { it.name == name } }
        )

        fun toPreference(items: Collection<MobileBottomNavItem>): String =
            sanitize(items).joinToString(",") { it.name }

        fun withItemEnabled(
            items: Collection<MobileBottomNavItem>,
            item: MobileBottomNavItem,
            enabled: Boolean
        ): List<MobileBottomNavItem> {
            val currentItems = sanitize(items)
            if (!enabled && currentItems.size == 1 && item in currentItems) return currentItems
            return sanitize(if (enabled) currentItems + item else currentItems - item)
        }
    }
}
