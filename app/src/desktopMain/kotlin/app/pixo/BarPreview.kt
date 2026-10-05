package app.pixo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import org.jetbrains.skia.EncodedImageFormat
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/**
 * 无头渲染脚手架：渲染 PixoFloatingBar 在 首页/账号 两个槽位的 静止+按压 帧，
 * 输出 PNG 到 build/barfix2/{tab0,tab1}/，用于对比胶囊对称性与容器撑出效果。
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main(args: Array<String>) {
    val outDir = File(if (args.isNotEmpty()) args[0] else "build/barfix2")
    outDir.mkdirs()
    val width = 1400
    val height = 480

    renderScene(outDir, "tab0", width, height, selectedIndex = 0, pressX = 370f)
    renderScene(outDir, "tab1", width, height, selectedIndex = 1, pressX = 1030f)
    println("preview frames -> " + outDir.absolutePath)
}

@OptIn(ExperimentalComposeUiApi::class)
private fun renderScene(outDir: File, name: String, width: Int, height: Int, selectedIndex: Int, pressX: Float) {
    val dir = File(outDir, name)
    dir.mkdirs()
    val scene = ImageComposeScene(width = width, height = height, density = Density(2f)) {
        PreviewHost(selectedIndex)
    }

    capture(scene, dir, "rest", 0L)

    scene.sendPointerEvent(PointerEventType.Press, Offset(pressX, 388f))
    for (i in intArrayOf(1, 5, 10, 15)) {
        capture(scene, dir, "press_%02d".format(i), i * 25_000_000L)
    }
    scene.sendPointerEvent(PointerEventType.Release, Offset(pressX, 388f))
    capture(scene, dir, "release_0", 600_000_000L)
    scene.close()
}

@Composable
private fun PreviewHost(selectedIndex: Int) {
    MiuixTheme(controller = remember { ThemeController(ColorSchemeMode.Light) }) {
        val backdrop = rememberLayerBackdrop {
            drawRect(Color(0xFFDFE8F2))
            drawContent()
        }
        Box(Modifier.fillMaxSize().background(Color(0xFFDFE8F2))) {
            Box(
                Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop),
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(start = 200.dp, top = 48.dp, end = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    repeat(6) { i ->
                        val mi = minOf(i, 5 - i)
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(if (i == 0) 72.dp else 48.dp)
                                .background(
                                    when (mi % 3) {
                                        0 -> Color(0xFF9FB4CC)
                                        1 -> Color(0xFFB9C9DC)
                                        else -> Color(0xFF8FA9C4)
                                    },
                                    RoundedCornerShape(12.dp),
                                ),
                        )
                    }
                }
            }
            PixoFloatingBar(
                backdrop = backdrop,
                liquidGlass = true,
                selectedIndex = selectedIndex,
                onPageSelect = {},
                onSizeChanged = {},
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun capture(scene: ImageComposeScene, dir: File, name: String, nanoTime: Long) {
    val img = scene.render(nanoTime)
    val bytes = img.encodeToData(EncodedImageFormat.PNG)?.bytes
    if (bytes != null) {
        File(dir, name + ".png").writeBytes(bytes)
    }
}
