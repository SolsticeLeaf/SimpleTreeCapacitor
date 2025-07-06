package sleaf.dev.mc.plugin.simpleTreeCapitator.listeners

import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockDamageEvent
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.plugin.Plugin
import org.bukkit.util.Vector
import org.bukkit.GameMode
import java.util.*
import org.bukkit.inventory.meta.Damageable

class TreeHighlightListener(private val plugin: Plugin) : Listener {

    // Хранит активные задачи подсветки для каждого игрока
    private val highlightingPlayers = mutableMapOf<UUID, BukkitRunnable>()

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

    // Проверяет, связан ли блок с основным стволом через другие блоки
    private fun isConnectedToMainTrunk(block: Block, mainTrunk: Block, visited: Set<Block>): Boolean {
        val queue = LinkedList<Block>()
        val checked = mutableSetOf<Block>()
        queue.add(block)
        checked.add(block)

        while (queue.isNotEmpty()) {
            val current = queue.poll()
            
            if (current == mainTrunk) return true

            val directions = listOf(
                Triple(1, 0, 0), Triple(-1, 0, 0),
                Triple(0, 1, 0), Triple(0, -1, 0),
                Triple(0, 0, 1), Triple(0, 0, -1)
            )

            for ((x, y, z) in directions) {
                val adjacent = current.world.getBlockAt(
                    current.x + x,
                    current.y + y,
                    current.z + z
                )

                if (isLogBlock(adjacent.type) && !checked.contains(adjacent) && visited.contains(adjacent)) {
                    queue.add(adjacent)
                    checked.add(adjacent)
                }
            }
        }

        return false
    }

    // Проверяет наличие бревна в указанном радиусе
    private fun hasNearbyLog(block: Block, radius: Int): Boolean {
        for (x in -radius..radius) {
            for (y in -radius..radius) {
                for (z in -radius..radius) {
                    val nearby = block.world.getBlockAt(
                        block.x + x,
                        block.y + y,
                        block.z + z
                    )
                    if (isLogBlock(nearby.type)) {
                        return true
                    }
                }
            }
        }
        return false
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

    // Создает обводку блока с помощью частиц
    private fun createBlockOutline(player: Player, block: Block) {
        val loc = block.location
        val whiteParticle = Particle.END_ROD
        val redParticle = Particle.REDSTONE
        val redDustOptions = org.bukkit.Particle.DustOptions(org.bukkit.Color.RED, 1.0f)

        // Создаем точки для обводки блока
        val edgePoints = mutableListOf<Vector>()
        
        // Нижняя грань (края)
        for (x in 0..1) {
            for (z in 0..1) {
                edgePoints.add(Vector(x.toDouble(), 0.0, z.toDouble()))
            }
        }
        
        // Верхняя грань (края)
        for (x in 0..1) {
            for (z in 0..1) {
                edgePoints.add(Vector(x.toDouble(), 1.0, z.toDouble()))
            }
        }
        
        // Вертикальные края
        for (x in 0..1) {
            for (z in 0..1) {
                for (y in 0..1) {
                    edgePoints.add(Vector(x.toDouble(), y.toDouble(), z.toDouble()))
                }
            }
        }

        // Отображаем частицы для краев
        for (point in edgePoints) {
            val particleLoc = loc.clone().add(point)
            player.spawnParticle(whiteParticle, particleLoc, 1, 0.0, 0.0, 0.0, 0.0)
        }

        // Отображаем частицы для граней
        val facePoints = mutableListOf<Vector>()
        
        // Нижняя грань
        for (i in 1..3) {
            facePoints.add(Vector(0.5, 0.0, i * 0.25)) // Передняя грань
            facePoints.add(Vector(0.5, 0.0, 1.0 - i * 0.25)) // Задняя грань
            facePoints.add(Vector(i * 0.25, 0.0, 0.5)) // Левая грань
            facePoints.add(Vector(1.0 - i * 0.25, 0.0, 0.5)) // Правая грань
        }

        // Верхняя грань
        for (i in 1..3) {
            facePoints.add(Vector(0.5, 1.0, i * 0.25)) // Передняя грань
            facePoints.add(Vector(0.5, 1.0, 1.0 - i * 0.25)) // Задняя грань
            facePoints.add(Vector(i * 0.25, 1.0, 0.5)) // Левая грань
            facePoints.add(Vector(1.0 - i * 0.25, 1.0, 0.5)) // Правая грань
        }

        // Боковые грани
        for (i in 1..3) {
            facePoints.add(Vector(0.0, i * 0.25, 0.5)) // Левая грань
            facePoints.add(Vector(1.0, i * 0.25, 0.5)) // Правая грань
            facePoints.add(Vector(0.5, i * 0.25, 0.0)) // Передняя грань
            facePoints.add(Vector(0.5, i * 0.25, 1.0)) // Задняя грань
        }

        // Отображаем частицы для граней
        for (point in facePoints) {
            val particleLoc = loc.clone().add(point)
            player.spawnParticle(redParticle, particleLoc, 1, 0.0, 0.0, 0.0, 0.0, redDustOptions)
        }
    }

    @EventHandler
    fun onBlockDamage(event: BlockDamageEvent) {
        val player = event.player
        val block = event.block
        val tool = player.inventory.itemInMainHand

        if (isTreeBlock(block.type) && player.isSneaking && hasTreeBreakerMeta(tool) && player.gameMode == GameMode.SURVIVAL) {
            // Отменяем существующую подсветку для этого игрока
            highlightingPlayers[player.uniqueId]?.cancel()
            
            // Создаем новую задачу подсветки
            val task = object : BukkitRunnable() {
                override fun run() {
                    // Проверяем, что игрок все еще онлайн и выполняет условия
                    if (!player.isOnline || !player.isSneaking || !hasTreeBreakerMeta(player.inventory.itemInMainHand) || player.gameMode != GameMode.SURVIVAL) {
                        cancel()
                        highlightingPlayers.remove(player.uniqueId)
                        return
                    }

                    val meta = tool.itemMeta as? Damageable ?: return
                    val maxDurability = tool.type.maxDurability
                    val damage = meta.damage
                    val remainingDurability = maxDurability - damage

                    // Находим все связанные блоки
                    val connectedBlocks = findConnectedTreeBlocks(block, remainingDurability * 2)
                    val logBlocks = connectedBlocks.count { isLogBlock(it.type) }

                    // Показываем частицы только если хватает прочности
                    if (logBlocks <= remainingDurability) {
                        // Создаем обводку для каждого блока
                        for (connectedBlock in connectedBlocks) {
                            createBlockOutline(player, connectedBlock)
                        }
                    }
                }
            }

            // Запускаем задачу каждые 5 тиков (1/4 секунды)
            task.runTaskTimer(plugin, 0L, 5L)
            highlightingPlayers[player.uniqueId] = task
        }
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        // Отменяем подсветку для игрока, если она активна
        highlightingPlayers[player.uniqueId]?.cancel()
        highlightingPlayers.remove(player.uniqueId)
    }
} 