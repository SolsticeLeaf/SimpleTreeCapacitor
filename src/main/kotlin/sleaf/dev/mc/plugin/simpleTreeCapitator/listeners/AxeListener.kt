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

class AxeListener : Listener {

    // Проверяет, является ли блок деревом или листвой
    private fun isTreeBlock(material: Material): Boolean {
        return material.name.contains("_LEAVES") || isLogBlock(material)
    }

    // Проверяет, является ли блок деревом (без листвы)
    private fun isLogBlock(material: Material): Boolean {
        return material.name.contains("_LOG") || material.name.contains("_WOOD")
    }

    // Проверяет наличие метки TreeBreaker в lore предмета
    private fun hasTreeBreakerMeta(item: ItemStack): Boolean {
        if (!item.hasItemMeta()) return false
        val meta = item.itemMeta
        return meta?.lore?.any { it.contains("TreeBreaker") } ?: false
    }

    @EventHandler
    fun blockBreakListener(event: BlockBreakEvent) {
        val player = event.player
        val tool = player.inventory.itemInMainHand
        val block = event.block

        // Проверяем все условия для срабатывания
        if (!event.isCancelled && isTreeBlock(block.type) && player.isSneaking && player.gameMode == GameMode.SURVIVAL) {
            if (hasTreeBreakerMeta(tool)) {
                val meta = tool.itemMeta as? Damageable ?: return
                val maxDurability = tool.type.maxDurability
                val damage = meta.damage
                val remainingDurability = maxDurability - damage

                // Находим все связанные блоки дерева с увеличенным максимальным радиусом
                val connectedBlocks = findConnectedTreeBlocks(block, remainingDurability * 2)
                // Подсчитываем только блоки дерева для проверки прочности
                val logBlocks = connectedBlocks.count { isLogBlock(it.type) }
                
                // Проверяем, хватает ли прочности на все блоки дерева
                if (logBlocks <= remainingDurability) {
                    // Увеличиваем урон инструмента на количество сломанных блоков дерева
                    meta.damage = damage + logBlocks
                    tool.itemMeta = meta
                    
                    // Если прочность достигла максимума, ломаем топор
                    if (meta.damage >= maxDurability) {
                        player.inventory.setItemInMainHand(null)
                        return
                    }

                    // Ломаем только бревна
                    for (connectedBlock in connectedBlocks) {
                        if (connectedBlock != block && isLogBlock(connectedBlock.type)) {
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

        if (!event.isCancelled && isTreeBlock(block.type) && player.isSneaking && player.gameMode == GameMode.SURVIVAL) {
            if (hasTreeBreakerMeta(tool)) {
                val meta = tool.itemMeta as? Damageable ?: return
                val maxDurability = tool.type.maxDurability
                val damage = meta.damage
                val remainingDurability = maxDurability - damage

                // Находим все связанные блоки дерева
                val connectedBlocks = findConnectedTreeBlocks(block, remainingDurability * 2)
                
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

    // Находит все связанные блоки дерева и листвы
    private fun findConnectedTreeBlocks(startBlock: Block, maxBlocks: Int): Set<Block> {
        val visited = mutableSetOf<Block>()
        val queue = LinkedList<Block>()
        queue.add(startBlock)
        visited.add(startBlock)

        // Определяем тип дерева из начального блока
        val treeType = getTreeType(startBlock.type)
        if (treeType == null) return visited

        // Находим ближайший ствол, если начальный блок - листва
        val mainTrunk = if (isLogBlock(startBlock.type)) {
            startBlock
        } else {
            findNearestLog(startBlock, 2) ?: return visited
        }

        // Сначала находим все бревна
        val logQueue = LinkedList<Block>()
        logQueue.add(mainTrunk)
        val visitedLogs = mutableSetOf<Block>()
        visitedLogs.add(mainTrunk)

        while (logQueue.isNotEmpty() && visitedLogs.size < maxBlocks) {
            val current = logQueue.poll()
            
            val directions = listOf(
                // Осевые направления
                Triple(1, 0, 0), Triple(-1, 0, 0),
                Triple(0, 1, 0), Triple(0, -1, 0),
                Triple(0, 0, 1), Triple(0, 0, -1),
                // Диагональные направления в горизонтальной плоскости
                Triple(1, 0, 1), Triple(1, 0, -1),
                Triple(-1, 0, 1), Triple(-1, 0, -1),
                // Диагональные направления с вертикальным смещением
                Triple(1, 1, 0), Triple(1, -1, 0),
                Triple(-1, 1, 0), Triple(-1, -1, 0),
                Triple(0, 1, 1), Triple(0, 1, -1),
                Triple(0, -1, 1), Triple(0, -1, -1),
                // Полностью диагональные направления
                Triple(1, 1, 1), Triple(1, 1, -1),
                Triple(1, -1, 1), Triple(1, -1, -1),
                Triple(-1, 1, 1), Triple(-1, 1, -1),
                Triple(-1, -1, 1), Triple(-1, -1, -1)
            )

            for ((x, y, z) in directions) {
                val adjacent = current.world.getBlockAt(
                    current.x + x,
                    current.y + y,
                    current.z + z
                )

                if (isLogBlock(adjacent.type) && !visitedLogs.contains(adjacent)) {
                    if (getTreeType(adjacent.type) == treeType) {
                        visitedLogs.add(adjacent)
                        logQueue.add(adjacent)
                    }
                }
            }
        }

        // Добавляем все найденные бревна в основной набор
        visited.addAll(visitedLogs)

        return visited
    }

    // Находит все листья, связанные с данным блоком бревна
    private fun findConnectedLeaves(logBlock: Block, visited: MutableSet<Block>, treeType: String) {
        val queue = LinkedList<Block>()
        queue.add(logBlock)

        while (queue.isNotEmpty()) {
            val current = queue.poll()
            
            val directions = listOf(
                // Осевые направления
                Triple(1, 0, 0), Triple(-1, 0, 0),
                Triple(0, 1, 0), Triple(0, -1, 0),
                Triple(0, 0, 1), Triple(0, 0, -1),
                // Диагональные направления
                Triple(1, 0, 1), Triple(1, 0, -1),
                Triple(-1, 0, 1), Triple(-1, 0, -1),
                Triple(1, 1, 0), Triple(1, -1, 0),
                Triple(-1, 1, 0), Triple(-1, -1, 0),
                Triple(0, 1, 1), Triple(0, 1, -1),
                Triple(0, -1, 1), Triple(0, -1, -1)
            )

            for ((x, y, z) in directions) {
                val adjacent = current.world.getBlockAt(
                    current.x + x,
                    current.y + y,
                    current.z + z
                )

                if (adjacent.type.name.contains("_LEAVES") && !visited.contains(adjacent)) {
                    if (getTreeType(adjacent.type) == treeType) {
                        visited.add(adjacent)
                        queue.add(adjacent)
                    }
                }
            }
        }
    }

    // Находит ближайший блок бревна в указанном радиусе
    private fun findNearestLog(block: Block, radius: Int): Block? {
        // Проверяем сначала соседние блоки по всем направлениям
        val directions = listOf(
            // Осевые направления
            Triple(1, 0, 0), Triple(-1, 0, 0),
            Triple(0, 1, 0), Triple(0, -1, 0),
            Triple(0, 0, 1), Triple(0, 0, -1),
            // Диагональные направления
            Triple(1, 0, 1), Triple(1, 0, -1),
            Triple(-1, 0, 1), Triple(-1, 0, -1),
            Triple(1, 1, 0), Triple(1, -1, 0),
            Triple(-1, 1, 0), Triple(-1, -1, 0),
            Triple(0, 1, 1), Triple(0, 1, -1),
            Triple(0, -1, 1), Triple(0, -1, -1)
        )

        for ((x, y, z) in directions) {
            val nearby = block.world.getBlockAt(
                block.x + x,
                block.y + y,
                block.z + z
            )
            if (isLogBlock(nearby.type)) {
                return nearby
            }
        }

        // Если не нашли в соседних блоках, проверяем остальные блоки в радиусе
        for (y in -radius..radius) {
            for (x in -radius..radius) {
                for (z in -radius..radius) {
                    // Пропускаем уже проверенные соседние блоки
                    if (x in -1..1 && y in -1..1 && z in -1..1) continue
                    
                    val nearby = block.world.getBlockAt(
                        block.x + x,
                        block.y + y,
                        block.z + z
                    )
                    if (isLogBlock(nearby.type)) {
                        return nearby
                    }
                }
            }
        }
        return null
    }

    // Получает тип дерева из материала
    private fun getTreeType(material: Material): String? {
        val name = material.name
        return when {
            name.contains("OAK") -> "OAK"
            name.contains("BIRCH") -> "BIRCH"
            name.contains("SPRUCE") -> "SPRUCE"
            name.contains("JUNGLE") -> "JUNGLE"
            name.contains("ACACIA") -> "ACACIA"
            name.contains("DARK_OAK") -> "DARK_OAK"
            name.contains("MANGROVE") -> "MANGROVE"
            name.contains("CHERRY") -> "CHERRY"
            else -> null
        }
    }
}