package org.inferna.icons

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object InfernaIcons {
    @JvmField
    val Dashboard: Icon = IconLoader.getIcon("/icons/infernaDashboard.svg", InfernaIcons::class.java)

    @JvmField
    val SpringBean: Icon = IconLoader.getIcon("/icons/springBeanGutter.svg", InfernaIcons::class.java)
}
