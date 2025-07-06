package sleaf.dev.mc.plugin.simpleTreeCapitator

import org.bukkit.plugin.java.JavaPlugin
import sleaf.dev.mc.plugin.simpleTreeCapitator.commands.OreBreakerCommand
import sleaf.dev.mc.plugin.simpleTreeCapitator.commands.TreeBreakerCommand
import sleaf.dev.mc.plugin.simpleTreeCapitator.listeners.*

class SimpleTreeCapitator : JavaPlugin() {

    override fun onEnable() {
        // Регистрируем листенеры
        server.pluginManager.registerEvents(AxeListener(), this)
        server.pluginManager.registerEvents(OreListener(), this)
        server.pluginManager.registerEvents(TreeHighlightListener(this), this)
        server.pluginManager.registerEvents(OreHighlightListener(this), this)

        // Регистрируем команды
        val treeBreakerCommand = TreeBreakerCommand()
        val oreBreakerCommand = OreBreakerCommand()
        
        getCommand("treebreaker")?.apply {
            setExecutor(treeBreakerCommand)
            tabCompleter = treeBreakerCommand
        }
        
        getCommand("orebreaker")?.apply {
            setExecutor(oreBreakerCommand)
            tabCompleter = oreBreakerCommand
        }
    }

    override fun onDisable() {
        // Логика выключения плагина
    }
}
