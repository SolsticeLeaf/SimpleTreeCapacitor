package sleaf.dev.mc.plugin.simpleTreeCapitator.listeners

import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDamageEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.GameMode
import org.bukkit.inventory.meta.Damageable
import java.util.*

class OreListener : Listener {

    // Проверяет, является ли блок рудой
    private fun isOreBlock(material: Material): Boolean {
        return material.name.endsWith("_ORE") || material.name.endsWith("_DEEPSLATE_ORE")
    }

    // Проверяет наличие метки OreBreaker в lore предмета
    private fun hasOreBreakerMeta(item: ItemStack): Boolean {
        if (!item.hasItemMeta()) return false
        val meta = item.itemMeta
        return meta?.lore?.any { it.contains("OreBreaker") } ?: false
    }

    @EventHandler
    fun blockBreakListener(event: BlockBreakEvent) {
        val player = event.player
        val tool = player.inventory.itemInMainHand
        val block = event.block

        if (!event.isCancelled && isOreBlock(block.type) && player.gameMode == GameMode.SURVIVAL) {
            if (hasOreBreakerMeta(tool)) {
                val meta = tool.itemMeta as? Damageable ?: return
                val maxDurability = tool.type.maxDurability
                val damage = meta.damage
                val remainingDurability = maxDurability - damage

                // Находим все связанные блоки руды
                val connectedBlocks = findConnectedOreBlocks(block, remainingDurability * 2)
                
                // Проверяем, хватает ли прочности на все блоки
                if (connectedBlocks.size <= remainingDurability) {
                    // Увеличиваем урон инструмента на количество сломанных блоков
                    meta.damage = damage + connectedBlocks.size
                    tool.itemMeta = meta
                    
                    // Если прочность достигла максимума, ломаем инструмент
                    if (meta.damage >= maxDurability) {
                        player.inventory.setItemInMainHand(null)
                        return
                    }

                    // Ломаем все связанные блоки
                    for (connectedBlock in connectedBlocks) {
                        if (connectedBlock != block) {
                            val drops = connectedBlock.drops
                            connectedBlock.type = Material.AIR
                            for (drop in drops) {
                                block.world.dropItemNaturally(block.location, drop)
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    fun blockDamageListener(event: BlockDamageEvent) {
        val player = event.player
        val tool = player.inventory.itemInMainHand
        val block = event.block

        if (!event.isCancelled && isOreBlock(block.type) && player.gameMode == GameMode.SURVIVAL) {
            if (hasOreBreakerMeta(tool)) {
                val meta = tool.itemMeta as? Damageable ?: return
                val maxDurability = tool.type.maxDurability
                val damage = meta.damage
                val remainingDurability = maxDurability - damage

                // Находим все связанные блоки руды
                val connectedBlocks = findConnectedOreBlocks(block, remainingDurability * 2)
                
                // Подсвечиваем только те блоки, которые будут сломаны
                for (connectedBlock in connectedBlocks) {
                    if (connectedBlock != block) {
                        connectedBlock.world.spawnParticle(
                            Particle.VILLAGER_HAPPY,
                            connectedBlock.location.add(0.5, 0.5, 0.5),
                            1, 0.0, 0.0, 0.0, 0.0
                        )
                    }
                }
            }
        }
    }

    // Находит все связанные блоки руды
    private fun findConnectedOreBlocks(startBlock: Block, maxBlocks: Int): Set<Block> {
        val visited = mutableSetOf<Block>()
        val queue = LinkedList<Block>()
        queue.add(startBlock)
        visited.add(startBlock)

        // Определяем тип руды из начального блока
        val oreType = getOreType(startBlock.type)
        if (oreType == null) return visited

        while (queue.isNotEmpty() && visited.size < maxBlocks) {
            val current = queue.poll()
            
            // Добавляем все возможные направления, включая диагонали
            val directions = listOf(
                // Оси
                Triple(1, 0, 0), Triple(-1, 0, 0),
                Triple(0, 1, 0), Triple(0, -1, 0),
                Triple(0, 0, 1), Triple(0, 0, -1),
                // Диагонали в плоскости XY
                Triple(1, 1, 0), Triple(-1, 1, 0),
                Triple(1, -1, 0), Triple(-1, -1, 0),
                // Диагонали в плоскости XZ
                Triple(1, 0, 1), Triple(-1, 0, 1),
                Triple(1, 0, -1), Triple(-1, 0, -1),
                // Диагонали в плоскости YZ
                Triple(0, 1, 1), Triple(0, -1, 1),
                Triple(0, 1, -1), Triple(0, -1, -1),
                // Диагонали в пространстве
                Triple(1, 1, 1), Triple(-1, 1, 1),
                Triple(1, -1, 1), Triple(-1, -1, 1),
                Triple(1, 1, -1), Triple(-1, 1, -1),
                Triple(1, -1, -1), Triple(-1, -1, -1)
            )

            for ((x, y, z) in directions) {
                val adjacent = current.world.getBlockAt(
                    current.x + x,
                    current.y + y,
                    current.z + z
                )

                if (isOreBlock(adjacent.type) && !visited.contains(adjacent)) {
                    // Проверяем, что тип руды совпадает
                    if (getOreType(adjacent.type) == oreType) {
                        visited.add(adjacent)
                        queue.add(adjacent)
                    }
                }
            }
        }

        return visited
    }

    // Получает тип руды из материала
    private fun getOreType(material: Material): String? {
        val name = material.name
        return when {
            name.contains("COAL") -> "COAL"
            name.contains("IRON") -> "IRON"
            name.contains("GOLD") -> "GOLD"
            name.contains("DIAMOND") -> "DIAMOND"
            name.contains("EMERALD") -> "EMERALD"
            name.contains("LAPIS") -> "LAPIS"
            name.contains("REDSTONE") -> "REDSTONE"
            name.contains("COPPER") -> "COPPER"
            name.contains("NETHERITE") -> "NETHERITE"
            name.contains("NETHER_GOLD") -> "NETHER_GOLD"
            name.contains("QUARTZ") -> "QUARTZ"
            name.contains("ANCIENT_DEBRIS") -> "ANCIENT_DEBRIS"
            else -> null
        }
    }
} 