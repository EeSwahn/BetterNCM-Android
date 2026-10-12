package com.example.bna.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.core.tween
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.bna.data.model.Playlist
import com.example.bna.ui.animation.SaltMotion
import androidx.compose.ui.layout.ContentScale

/**
 * 椒盐音乐（Salt Player）平板主界面 —— 左侧导航栏。
 *
 * 逆向依据（v11.0.4，`androidx.core.wu0` / `su0`）：
 *  - 左栏骨架：`wu0.m6983` — 顶部主题行 + 两组圆角卡片
 *  - 条目行：`wu0.m6985` — padding 16dp；图标 24dp（tint 彩色）；文字区 start 缩进 36dp、单行省略号
 *  - 卡片容器：`p24.m4782` — 圆角走主题
 *
 * 本项目为在线网易云，无本地曲库，因此按需求只保留两项真实可用的栏目：
 *  - 「音乐搜索」（对应 salt 的歌曲页 route `song`）
 *  - 「歌单」标题 + 其下动态展开的登录用户歌单列表
 * 原有的主题三选一行因无常驻需求已移除（需求：「没用的全删」）。
 */

/** 左栏一个固定条目。配色取自 salt `su0.java` 的 `m3.m3811(r,g,b,255)`。 */
data class SidebarItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val color: Color
)

/** 固定栏目（仅保留真实可用的两项）。 */
object SidebarItems {
    val primary = listOf(
        SidebarItem("search", "音乐搜索", Icons.Filled.Search, Color(50, 200, 90))
    )
}

/** 歌单区的标题行（不可点击）。 */
private const val PLAYLIST_HEADER_ID = "playlist_header"

/**
 * 条目文字区相对行首的缩进。
 *
 * 【观测】salt `wu0.m6985` 的文字区是 **start 缩进 36dp**（绝对偏移，从行首算起），
 * 而不是"图标宽度 + 间距"的算式。此前的 `width(36.dp - 24.dp)` 把绝对缩进当成了算式。
 *
 * 这里保留绝对缩进语义：图标槽 24dp + 间距 = [SidebarTextIndent]，
 * 于是文字左边缘 = 16dp(行内边距) + 36dp。
 */
private val SidebarTextIndent = 36.dp

/** 图标槽宽度（salt `wu0.m6985`：图标 24dp）。 */
private val SidebarIconSlot = 24.dp

/** 图标槽与文字之间的间距，使文字左边缘落在 [SidebarTextIndent] 处。 */
private val SidebarIconTextGap = SidebarTextIndent - SidebarIconSlot

/**
 * 卡片与条目行**共用**的圆角形状。
 *
 * ★ 之所以共用：条目行是卡片里唯一的"底"，两者若用不同圆角（如卡片 16dp、行 12dp），
 * 选中时就会看到嵌套的两个圆角色块（"两个底"）。统一成同一形状后，
 * 行的底与卡的边界完全重合，视觉上只有一个底。
 */
private val SidebarItemShape = RoundedCornerShape(12.dp)

/** 卡片左右外边距（条目行随之收进 [SidebarCardHorizontalPadding] 之后对齐）。 */
private val SidebarCardHorizontalPadding = 12.dp

/**
 * 左栏完整内容：固定栏目（音乐搜索）+ 登录用户歌单列表。
 *
 * @param width 左栏宽度（salt：平板 280/360dp、手机抽屉 200dp）
 */
@Composable
fun SidebarMenu(
    selectedId: String,
    onSelect: (String) -> Unit,
    width: Int = 280,
    playlists: List<Playlist> = emptyList(),
    playlistsLoading: Boolean = false,
    playlistError: String? = null,
    onRefreshPlaylists: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .width(width.dp)
            .fillMaxHeight()
            .background(LocalHomeColors.current.background),
        contentPadding = PaddingValues(top = 8.dp, bottom = LocalHomeListBottomPadding.current)
    ) {
        // ── 固定栏目：音乐搜索 ──
        item(key = "fixed") {
            SidebarCard {
                SidebarItems.primary.forEach { item ->
                    SidebarRow(
                        item = item,
                        selected = item.id == selectedId,
                        onClick = { onSelect(item.id) }
                    )
                }
            }
        }

        // ── 歌单分组标题 ──
        item(key = PLAYLIST_HEADER_ID) {
            SidebarSectionHeader(
                title = "我的歌单",
                loading = playlistsLoading,
                onRefresh = onRefreshPlaylists
            )
        }

        // ── 歌单列表（每个歌单一项） ──
        when {
            playlistsLoading && playlists.isEmpty() -> {
                item(key = "playlists_loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = LocalHomeColors.current.accent,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            !playlistError.isNullOrBlank() && playlists.isEmpty() -> {
                item(key = "playlists_error") {
                    SidebarHint(text = playlistError, color = Color(0xFFE0533D))
                }
            }

            playlists.isEmpty() -> {
                item(key = "playlists_empty") {
                    SidebarHint(text = "暂无歌单", color = Color(0xFF6A6C6E))
                }
            }

            else -> {
                items(playlists, key = { "pl_${it.id}" }) { playlist ->
                    PlaylistRow(
                        playlist = playlist,
                        selected = selectedId == "playlist_${playlist.id}",
                        onClick = { onSelect("playlist_${playlist.id}") }
                    )
                }
            }
        }

        item(key = "bottom_spacer") { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

/** 分组标题行：文字 + 刷新按钮。 */
@Composable
private fun SidebarSectionHeader(
    title: String,
    loading: Boolean,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 28.dp, end = 16.dp, top = 16.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color(0xFF8A8C8E),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(enabled = !loading, onClick = onRefresh),
            contentAlignment = Alignment.Center
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = LocalHomeColors.current.accent,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "刷新歌单",
                    tint = Color(0xFF8A8C8E),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun SidebarHint(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        fontSize = 12.sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp)
    )
}

/**
 * 圆角卡片容器 —— 对齐 salt `androidx.core.p24.m4782`（`$this$RoundedColumn`）。
 *
 * 【观测】`p24.m4782` 的动作序列：
 *  1. 从 `uj2.f13301`(LocalShapes) 取形状；
 *  2. `AbstractC0903.m8907(modifier, 1.0f, 边缘色, 形状)` —— 一个**跟随动画的描边/尺寸**修饰符，
 *     其内部是 `new C1563(width, brush, shape)` → `toString()` = `"BorderModifierNodeElement(width=...)"`；
 *  3. `dx0.m1615(modifier, 动画进度值)` 再套内边距。
 *
 * 本项目用等效 Compose 表达：**形状裁切 + 动画内边距 + 随选中态过渡的描边**。
 * 描边宽度与内边距都走 salt 的 `FastSpatial` 规格，避免"硬切"。
 *
 * ## ★ 只保留"一层底"
 * 卡片自身**不再画背景色**（只保留描边与形状）。原因：条目行 [SidebarRow] 自己就带
 * 选中态背景，若卡片再铺一层底色，选中的那一行就会出现「卡片底 + 行底」两层大小/圆角
 * 都不同的色块 —— 视觉上就是"两个底"。现在选中高亮**只由行背景表达**。
 *
 * @param borderWidth 目标描边宽度（选中卡片更亮/更粗）
 * @param shape       卡片圆角形状；条目行复用**同一个** [shape]，保证底的大小形状一致
 */
@Composable
private fun SidebarCard(
    borderWidth: androidx.compose.ui.unit.Dp = 0.dp,
    shape: androidx.compose.ui.graphics.Shape = SidebarItemShape,
    content: @Composable ColumnScope.() -> Unit
) {
    val animatedBorder by animateDpAsState(
        targetValue = borderWidth,
        animationSpec = SaltMotion.FastSpatialDp,
        label = "cardBorder"
    )
    val borderColor by animateColorAsState(
        targetValue = if (borderWidth > 0.dp) Color.White.copy(alpha = 0.14f) else Color.Transparent,
        animationSpec = tween(240, easing = HomeEase),
        label = "cardBorderColor"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SidebarCardHorizontalPadding)
            .clip(shape)
            // 不铺底色：底色只由条目行提供，避免出现"两个底"
            .then(
                if (animatedBorder > 0.dp) {
                    Modifier.border(width = animatedBorder, color = borderColor, shape = shape)
                } else {
                    Modifier
                }
            )
            .padding(vertical = 6.dp),
        content = content
    )
}

/**
 * 单个条目行。
 *
 * salt `wu0.m6985`【观测】：整行 `clickable` → padding 16dp；图标 24dp；
 * **文字区 start 缩进 36dp**（相对行首，不是"图标 24 + 间距 12"）。
 *
 * 选中态在本项目中用动画过渡（背景 alpha + 文字颜色），走 salt `FastSpatial`/`FastEffects`，
 * 替代此前的硬切。
 */
@Composable
private fun SidebarRow(
    item: SidebarItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val shift by animateDpAsState(if (selected) 4.dp else 0.dp, tween(320, easing = HomeEase), label = "selectionShift")
    val background by animateColorAsState(
        targetValue = if (selected) LocalHomeColors.current.accent.copy(alpha = 0.14f) else Color.Transparent,
        animationSpec = tween(240, easing = HomeEase),
        label = "rowBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) Color.White else Color(0xFFC8C8C8),
        animationSpec = tween(240, easing = HomeEase),
        label = "rowText"
    )
    // 选中时图标轻微放大，模仿 salt 的按压/选中反馈
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1.0f,
        animationSpec = SaltMotion.FastSpatial,
        label = "rowIconScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .homePress(interaction)
            .offset(x = shift)
            .clip(SidebarItemShape)
            .background(background)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = LocalHomeColors.current.accent,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                }
        )

        // ★ salt：文字区 start 缩进 36dp（相对行首）。图标已占 24dp，故这里补 [SidebarIconTextGap]
        Spacer(modifier = Modifier.width(SidebarIconTextGap))

        Text(
            text = item.label,
            color = textColor,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * 歌单条目行：封面 + 歌单名（+ 曲目数）。
 *
 * 沿用 salt 的行规格：padding 16dp、图标区 24dp、**文字缩进 36dp**（见 [SidebarTextIndent]）。
 * 仅把图标换成歌单封面缩略图。选中态同样动画过渡。
 */
@Composable
private fun PlaylistRow(
    playlist: Playlist,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val shift by animateDpAsState(if (selected) 4.dp else 0.dp, tween(320, easing = HomeEase), label = "selectionShift")
    val background by animateColorAsState(
        targetValue = if (selected) LocalHomeColors.current.accent.copy(alpha = 0.14f) else Color.Transparent,
        animationSpec = tween(240, easing = HomeEase),
        label = "plRowBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) Color.White else Color(0xFFC8C8C8),
        animationSpec = tween(240, easing = HomeEase),
        label = "plRowText"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SidebarCardHorizontalPadding)
            .homePress(interaction)
            .offset(x = shift)
            .clip(SidebarItemShape)
            .background(background)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 封面槽固定 24dp，与 salt 的文字缩进基准一致
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            if (playlist.isLikedPlaylist) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = Color(255, 60, 50),
                    modifier = Modifier.size(16.dp)
                )
            } else if (playlist.coverUrl.isNotEmpty()) {
                AsyncImage(
                    model = playlist.coverUrl + "?param=60y60",
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = null,
                    tint = Color(0, 120, 255),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(SidebarIconTextGap))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                color = textColor,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (playlist.trackCount > 0) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${playlist.trackCount} 首",
                    color = Color(0xFF6A6C6E),
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
    }
}
