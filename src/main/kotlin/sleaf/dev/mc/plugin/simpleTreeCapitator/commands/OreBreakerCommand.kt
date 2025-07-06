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

class OreBreakerCommand : CommandExecutor, TabCompleter {
    // Список доступных подкоманд
    private val subCommands = listOf("create", "get")
    
    // Список доступных типов топоров
    private val axeTypes = listOf(
        "NETHERITE_PICKAXE",
        "DIAMOND_PICKAXE",
        "IRON_PICKAXE",
        "GOLDEN_PICKAXE"
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
            sender.sendMessage("${ChatColor.RED}Использование: /treebreaker <create|get> [тип кирки]")
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
        
        // Проверяем, нет ли уже метки OreBreaker
        if (lore.contains("OreBreaker")) {
            player.sendMessage("${ChatColor.YELLOW}Этот предмет уже имеет метку OreBreaker!")
            return
        }

        // Добавляем метку OreBreaker
        lore.add("OreBreaker")
        meta.lore = lore
        item.itemMeta = meta

        player.sendMessage("${ChatColor.GREEN}Метка OreBreaker успешно добавлена к предмету!")
    }

    private fun handleGet(player: Player, args: Array<out String>) {
        if (args.size < 2) {
            player.sendMessage("${ChatColor.RED}Укажите тип кирки: NETHERITE/IRON/GOLDEN/DIAMOND PICKAXE")
            return
        }

        // Получаем тип кирки из аргументов
        val pickAxeType = args[1].uppercase()
        val material = when (pickAxeType) {
            "NETHERITE_PICKAXE" -> Material.NETHERITE_PICKAXE
            "DIAMOND_PICKAXE" -> Material.DIAMOND_PICKAXE
            "IRON_PICKAXE" -> Material.IRON_PICKAXE
            "GOLDEN_PICKAXE" -> Material.GOLDEN_PICKAXE
            else -> {
                player.sendMessage("${ChatColor.RED}Неверный тип кирки. Используйте: NETHERITE/IRON/GOLDEN/DIAMOND PICKAXE")
                return
            }
        }

        // Создаем топор с меткой TreeBreaker
        val axe = ItemStack(material)
        val meta = axe.itemMeta ?: return
        meta.lore = listOf("OreBreaker")
        axe.itemMeta = meta

        // Выдаем топор игроку
        player.inventory.addItem(axe)
        player.sendMessage("${ChatColor.GREEN}Вы получили кирку $pickAxeType с меткой OreBreaker!")
    }
} 