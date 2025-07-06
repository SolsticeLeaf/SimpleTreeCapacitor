package sleaf.dev.mc.plugin.simpleTreeCapitator.commands

import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

class TreeBreakerCommand : CommandExecutor, TabCompleter {
    // Список доступных подкоманд
    private val subCommands = listOf("create", "get")
    
    // Список доступных типов топоров
    private val axeTypes = listOf(
        "NETHERITE_AXE",
        "DIAMOND_AXE",
        "IRON_AXE",
        "GOLDEN_AXE"
    )

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        // Проверяем, что команду выполняет игрок
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}Эта команда может быть использована только игроком!")
            return true
        }

        // Проверяем права оператора
        if (!sender.isOp) {
            sender.sendMessage("${ChatColor.RED}У вас нет прав для использования этой команды!")
            return true
        }

        // Проверяем наличие подкоманды
        if (args.isEmpty()) {
            sender.sendMessage("${ChatColor.RED}Использование: /treebreaker <create|get> [тип топора]")
            return true
        }

        when (args[0].lowercase()) {
            "create" -> handleCreate(sender)
            "get" -> handleGet(sender, args)
            else -> {
                sender.sendMessage("${ChatColor.RED}Неизвестная подкоманда. Используйте create или get")
                return true
            }
        }

        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        // Если отправитель не игрок или не оператор, не предлагаем автодополнение
        if (sender !is Player || !sender.isOp) {
            return emptyList()
        }

        return when (args.size) {
            // Предлагаем подкоманды
            1 -> subCommands.filter { it.startsWith(args[0].lowercase()) }
            // Предлагаем типы топоров только для подкоманды get
            2 -> if (args[0].equals("get", ignoreCase = true)) {
                axeTypes.filter { it.startsWith(args[1].uppercase()) }
            } else {
                emptyList()
            }
            // Больше аргументов не нужно
            else -> emptyList()
        }
    }

    private fun handleCreate(player: Player) {
        // Проверяем наличие предмета в руке
        val item = player.inventory.itemInMainHand
        if (item.type.isAir) {
            player.sendMessage("${ChatColor.RED}Возьмите предмет в руку!")
            return
        }

        // Получаем метаданные предмета
        val meta = item.itemMeta ?: return
        val lore = meta.lore?.toMutableList() ?: mutableListOf()
        
        // Проверяем, нет ли уже метки TreeBreaker
        if (lore.contains("TreeBreaker")) {
            player.sendMessage("${ChatColor.YELLOW}Этот предмет уже имеет метку TreeBreaker!")
            return
        }

        // Добавляем метку TreeBreaker
        lore.add("TreeBreaker")
        meta.lore = lore
        item.itemMeta = meta

        player.sendMessage("${ChatColor.GREEN}Метка TreeBreaker успешно добавлена к предмету!")
    }

    private fun handleGet(player: Player, args: Array<out String>) {
        if (args.size < 2) {
            player.sendMessage("${ChatColor.RED}Укажите тип топора: NETHERITE/IRON/GOLD/DIAMOND AXE")
            return
        }

        // Получаем тип топора из аргументов
        val axeType = args[1].uppercase()
        val material = when (axeType) {
            "NETHERITE_AXE" -> Material.NETHERITE_AXE
            "DIAMOND_AXE" -> Material.DIAMOND_AXE
            "IRON_AXE" -> Material.IRON_AXE
            "GOLDEN_AXE" -> Material.GOLDEN_AXE
            else -> {
                player.sendMessage("${ChatColor.RED}Неверный тип топора. Используйте: NETHERITE/IRON/GOLDEN/DIAMOND AXE")
                return
            }
        }

        // Создаем топор с меткой TreeBreaker
        val axe = ItemStack(material)
        val meta = axe.itemMeta ?: return
        meta.lore = listOf("TreeBreaker")
        axe.itemMeta = meta

        // Выдаем топор игроку
        player.inventory.addItem(axe)
        player.sendMessage("${ChatColor.GREEN}Вы получили топор $axeType с меткой TreeBreaker!")
    }
} 