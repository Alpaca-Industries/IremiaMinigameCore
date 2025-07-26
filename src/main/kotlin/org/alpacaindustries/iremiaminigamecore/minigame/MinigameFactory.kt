package org.alpacaindustries.iremiaminigamecore.minigame

interface MinigameFactory {
    fun createMinigame(id: String, manager: MinigameManager): Minigame?
}
