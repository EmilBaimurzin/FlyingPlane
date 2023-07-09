package com.fly.flyingplane.ui.game

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GameViewModel(private val initialXY: Pair<Int, Int>): ViewModel() {
    private val _playerXY = MutableLiveData(initialXY.first / 2f to initialXY.second / 2f)
    val playerXY: LiveData<Pair<Float, Float>> = _playerXY

    private var timer = false

    private val _health = MutableLiveData(12)
    val health: LiveData<Int> = _health

    private val _scores = MutableLiveData(0)
    val scores: LiveData<Int> = _scores

    var enemyList: MutableList<Pair<Float, Float>> = mutableListOf()
    var gameState = true

    fun increaseScores(callback: () -> Unit) {
        viewModelScope.launch {
            if (!timer) {
                callback.invoke()
                val newScores = _scores.value!! + 10
                _scores.postValue(newScores)
                timer = true
                delay(1000)
                timer = false
            }
        }
    }

    fun useSmallHealth() {
        val newHealth = if (_health.value!! <= 10) _health.value!! + 2 else 12
        _health.postValue(newHealth)
    }

    fun useBigHealth() {
        _health.postValue(12)
    }

    fun removeHealth() {
        val newHealth = if (_health.value!! == 0) 0 else _health.value!! - 1
        _health.postValue(newHealth)
    }

    fun setPlayerXY(x: Float, y: Float) {
        _playerXY.postValue(x to y)
    }

    fun setEnemyPosition(index: Int, x: Float, y: Float) {
        enemyList[index] = Pair(x, y)
    }

    fun removeEnemy(index: Int) {
        enemyList.removeAt(index)
    }

    fun addEnemy(x: Float, y: Float) {
        enemyList.add(Pair(x, y))
    }

}

class GameViewModelFactory(private val initialXY: Pair<Int, Int>): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return GameViewModel(initialXY) as T
    }
}