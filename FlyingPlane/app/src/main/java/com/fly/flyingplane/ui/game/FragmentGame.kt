package com.fly.flyingplane.ui.game

import android.app.ActionBar.LayoutParams
import android.content.Context.MODE_PRIVATE
import android.graphics.Point
import android.graphics.Rect
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.contains
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.airbnb.lottie.LottieAnimationView
import com.fly.flyingplane.R
import com.fly.flyingplane.core.soundClickListener
import com.fly.flyingplane.databinding.FragmentGameBinding
import com.fly.flyingplane.ui.other.ViewBindingFragment
import io.github.hyuwah.draggableviewlib.DraggableListener
import io.github.hyuwah.draggableviewlib.DraggableView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


class FragmentGame : ViewBindingFragment<FragmentGameBinding>(FragmentGameBinding::inflate) {
    private val sharedPrefs by lazy {
        requireActivity().getSharedPreferences("SHARED_PREFS", MODE_PRIVATE)
    }
    private lateinit var playerPlaneView: DraggableView<ImageView>
    private val soundPool = SoundPool(10, AudioManager.STREAM_MUSIC, 0)
    private val soundMap = HashMap<Int, Int>()
    private lateinit var viewModel: GameViewModel
    private val xy by lazy {
        val display = requireActivity().windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        Pair(size.x, size.y)
    }
    private lateinit var gameScope: CoroutineScope
    private val scopeList = mutableListOf<CoroutineScope>()
    private val enemyList = mutableListOf<View>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModel()
        setupPlayerView()
        soundMap.put(1, soundPool.load(requireContext(), R.raw.shot, 1))
        soundMap.put(2, soundPool.load(requireContext(), R.raw.damage, 2))
        soundMap.put(3, soundPool.load(requireContext(), R.raw.explosion, 2))
        soundMap.put(4, soundPool.load(requireContext(), R.raw.heal, 2))
        soundMap.put(5, soundPool.load(requireContext(), R.raw.win, 2))
        soundMap.put(6, soundPool.load(requireContext(), R.raw.lose, 2))

        binding.menuButton.soundClickListener {
            findNavController().popBackStack()
        }

        viewModel.health.observe(viewLifecycleOwner) { value ->
            if (value == 0 && viewModel.gameState) {
                binding.playerPlane.visibility = View.GONE
                viewModel.gameState = false
                endGame()
            }
            binding.healthLayout.removeAllViews()
            repeat(value) { index ->
                val healthPoint = index + 1
                val healthView = View(requireContext())
                healthView.layoutParams =
                    LinearLayout.LayoutParams(dpToPx(5), LayoutParams.MATCH_PARENT).apply {
                        marginStart = dpToPx(1)
                        marginEnd = dpToPx(1)
                        topMargin = dpToPx(6)
                        bottomMargin = dpToPx(6)
                    }
                val bg = when (healthPoint) {
                    in (1..4) -> R.drawable.img_red_health
                    in (5..8) -> R.drawable.img_yellow_health
                    else -> R.drawable.img_green_health
                }
                healthView.background = ResourcesCompat.getDrawable(resources, bg, null)
                binding.healthLayout.addView(healthView)
            }
        }

        viewModel.scores.observe(viewLifecycleOwner) {
            binding.scoresTextView.text = it.toString()
        }
    }

    private fun endGame() {
        gameScope.cancel()
        val record = sharedPrefs.getLong("RECORD", 0)
        val isRecord = viewModel.scores.value!! > record
        val playerXY = viewModel.playerXY.value!!
        spawnExplosion(playerXY.first, playerXY.second)

        if (isRecord) {
            soundWin()
            sharedPrefs.edit().putLong("RECORD", viewModel.scores.value!!.toLong()).apply()
        } else {
            soundLose()
        }
        findNavController().navigate(
            FragmentGameDirections.actionFragmentGameToDialogScores(
                isRecord,
                viewModel.scores.value!!.toLong()
            )
        )
    }

    private fun respawnEnemies() {
        viewModel.enemyList.forEachIndexed { index, enemyPosition ->
            val a = xy.second - enemyPosition.second
            val b = xy.second / a
            val c = 3000 / b
            val duration = c.toLong()
            spawnEnemy(index, enemyPosition.first, enemyPosition.second, false, duration)
        }
    }

    private fun generateEnemies() {
        gameScope.launch {
            while (true) {
                delay(3000)
                val randomX = ((0..xy.first - dpToPx(80)).random()).toFloat()
                val y = (0 - dpToPx(80)).toFloat()
                spawnEnemy(enemyList.size, randomX, y, true, 3000)
            }
        }
    }

    private fun spawnEnemy(
        enemyIndex: Int, x: Float,
        y: Float,
        addToViewModel: Boolean,
        duration: Long
    ) {
        gameScope.launch {
            val repeatScope = CoroutineScope(Dispatchers.Default)
            scopeList.add(repeatScope)
            val enemyView = ImageView(requireContext())
            enemyView.layoutParams = ViewGroup.LayoutParams(dpToPx(80), dpToPx(80))
            enemyView.setImageResource(R.drawable.img_enemy_plane)
            binding.gameLayout.addView(enemyView)
            enemyList.add(enemyView)
            enemyView.x = x
            enemyView.y = y
            if (addToViewModel) viewModel.addEnemy(x = x, y = y)
            enemyView.animate()
                .setDuration(duration)
                .y(xy.second.toFloat())
                .withEndAction {
                    if (enemyList.isNotEmpty() && enemyList.size != enemyIndex) {
                        if (enemyList[enemyIndex] == enemyView) {
                            repeatScope.cancel()
                            try {
                                enemyList.removeAll { it == enemyView }
                                if (binding.gameLayout.contains(enemyView)) {
                                    binding.gameLayout.removeView(enemyView)
                                }
                                viewModel.removeEnemy(enemyIndex)
                            } catch (_: Throwable) {
                            }
                        }
                    }
                }
            repeatScope.launch {
                repeat(28) {
                    delay(100)
                    try {
                        viewModel.setEnemyPosition(index = enemyIndex, x = x, y = enemyView.y)
                    } catch (_: Throwable) {
                    }
                }
            }
        }
    }

    private fun spawnSmallKit() {
        gameScope.launch {
            while (true) {
                delay(25000)
                val kitView = ImageView(requireContext())
                kitView.layoutParams = ViewGroup.LayoutParams(dpToPx(40), dpToPx(40))
                kitView.setImageResource(R.drawable.img_first_aid_kit_small)
                binding.gameLayout.addView(kitView)
                val randomX = ((0..xy.first - dpToPx(80)).random()).toFloat()
                val y = (0 - dpToPx(40)).toFloat()
                kitView.x = randomX
                kitView.y = y
                addKitListener(kitView, false)
                kitView.animate()
                    .setDuration(6000)
                    .y(xy.second.toFloat())
                    .withEndAction {
                        try {
                            if (binding.gameLayout.contains(kitView)) {
                                binding.gameLayout.removeView(kitView)
                            }
                        } catch (_: Throwable) {
                        }
                    }
            }
        }
    }

    private fun spawnBigKit() {
        gameScope.launch {
            while (true) {
                delay(60000)
                val kitView = ImageView(requireContext())
                kitView.layoutParams = ViewGroup.LayoutParams(dpToPx(40), dpToPx(40))
                kitView.setImageResource(R.drawable.img_first_aid_kit_big)
                binding.gameLayout.addView(kitView)
                val randomX = ((0..xy.first - dpToPx(80)).random()).toFloat()
                val y = (0 - dpToPx(40)).toFloat()
                kitView.x = randomX
                kitView.y = y
                addKitListener(kitView, true)
                kitView.animate()
                    .setDuration(6000)
                    .y(xy.second.toFloat())
                    .withEndAction {
                        try {
                            if (binding.gameLayout.contains(kitView)) {
                                binding.gameLayout.removeView(kitView)
                            }
                        } catch (_: Throwable) {
                        }
                    }
            }
        }
    }

    private fun generatePlayerAttack() {
        gameScope.launch {
            while (true) {
                val shotView = ImageView(requireContext())
                shotView.layoutParams = ViewGroup.LayoutParams(dpToPx(20), dpToPx(20))
                shotView.setImageResource(R.drawable.img_bullet)
                binding.gameLayout.addView(shotView)
                shotView.x = viewModel.playerXY.value!!.first + dpToPx(30)
                shotView.y = viewModel.playerXY.value!!.second
                addPlayerAttackListener(shotView)
                shotView.animate()
                    .setDuration(1000)
                    .y(viewModel.playerXY.value!!.second - xy.second)
                    .withEndAction {
                        gameScope.launch {
                            try {
                                if (binding.gameLayout.contains(shotView)) {
                                    binding.gameLayout.removeView(shotView)
                                }
                            } catch (_:Throwable) {}
                        }
                    }
                soundShot()
                delay(500)
            }
        }
    }

    private fun generateEnemyAttack() {
        gameScope.launch {
            while (true) {
                viewModel.enemyList.forEachIndexed { index, enemyPosition ->
                    val shotView = ImageView(requireContext())
                    shotView.layoutParams = ViewGroup.LayoutParams(dpToPx(20), dpToPx(20))
                    shotView.setImageResource(R.drawable.img_bullet_enemy)
                    binding.gameLayout.addView(shotView)
                    shotView.x = enemyPosition.first + dpToPx(30)
                    shotView.y = enemyPosition.second + dpToPx(120)
                    addEnemyAttackListener(shotView)
                    shotView.animate()
                        .setDuration(1000)
                        .y(enemyPosition.second + xy.second)
                        .withEndAction {
                            try {
                                if (binding.gameLayout.contains(shotView)) {
                                    binding.gameLayout.removeView(shotView)
                                }
                            } catch (_:Throwable) {}
                        }
                }
                delay(500)
            }
        }
    }

    private fun addEnemyAttackListener(view: View) {
        gameScope.launch {
            val viewTreeObserver = binding.gameLayout.viewTreeObserver
            val listener = object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    try {
                        val rect1 = Rect()
                        val rect2 = Rect()
                        binding.playerPlane.getGlobalVisibleRect(rect1)
                        view.getGlobalVisibleRect(rect2)
                        val isIntersecting = rect1.intersect(rect2)
                        if (isIntersecting) {
                            soundDamage()
                            viewModel.removeHealth()
                            viewTreeObserver.removeOnPreDrawListener(this)
                        }
                    } catch (_: Throwable) {
                    }
                    return true
                }
            }
            viewTreeObserver.addOnPreDrawListener(listener)
            delay(1000)
            viewTreeObserver.removeOnPreDrawListener(listener)
        }
    }

    private fun addKitListener(view: View, isBig: Boolean) {
        gameScope.launch {
            val viewTreeObserver = binding.gameLayout.viewTreeObserver
            val listener = object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    try {
                        val rect1 = Rect()
                        val rect2 = Rect()
                        binding.playerPlane.getGlobalVisibleRect(rect1)
                        view.getGlobalVisibleRect(rect2)
                        val isIntersecting = rect1.intersect(rect2)
                        if (isIntersecting) {
                            if (binding.gameLayout.contains(view)) {
                                binding.gameLayout.removeView(view)
                            }
                            soundHeal()
                            if (isBig) {
                                viewModel.useBigHealth()
                            } else {
                                viewModel.useSmallHealth()
                            }
                            viewTreeObserver.removeOnPreDrawListener(this)
                        }
                    } catch (_: Throwable) {
                    }
                    return true
                }
            }
            viewTreeObserver.addOnPreDrawListener(listener)
            delay(6000)
            viewTreeObserver.removeOnPreDrawListener(listener)
        }
    }

    private fun addPlayerAttackListener(view: View) {
        gameScope.launch {
            val viewTreeObserver = binding.gameLayout.viewTreeObserver
            enemyList.forEachIndexed { index, enemyView ->
                val listener = object : ViewTreeObserver.OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        val rect1 = Rect()
                        val rect2 = Rect()
                        enemyView.getGlobalVisibleRect(rect1)
                        view.getGlobalVisibleRect(rect2)
                        val isIntersecting = rect1.intersect(rect2)
                        if (isIntersecting) {
                            try {
                                viewModel.increaseScores {
                                    spawnExplosion(enemyView.x, enemyView.y)
                                    soundExplosion()
                                }
                                scopeList[index].cancel()
                                scopeList.removeAll { it == scopeList[index] }
                                if (binding.gameLayout.contains(enemyView)) {
                                    binding.gameLayout.removeView(enemyView)
                                }
                                enemyList.removeAll { it == enemyView }
                                viewModel.removeEnemy(index)
                                viewTreeObserver.removeOnPreDrawListener(this)
                            } catch (_: Throwable) {
                            }
                        }
                        return true
                    }
                }
                viewTreeObserver.addOnPreDrawListener(listener)
                delay(1000)
                viewTreeObserver.removeOnPreDrawListener(listener)
            }
        }
    }

    private fun soundDamage() {
        if (sharedPrefs.getBoolean("SFX", true)) {
            val soundId = soundMap[2]!!
            soundPool.play(soundId, 1f, 1f, 0, 0, 1f)
        }
    }

    private fun soundExplosion() {
        if (sharedPrefs.getBoolean("SFX", true)) {
            val soundId = soundMap[3]!!
            soundPool.play(soundId, 1f, 1f, 0, 0, 1f)
        }
    }

    private fun soundHeal() {
        if (sharedPrefs.getBoolean("SFX", true)) {
            val soundId = soundMap[4]!!
            soundPool.play(soundId, 1f, 1f, 0, 0, 1f)
        }
    }

    private fun soundWin() {
        if (sharedPrefs.getBoolean("SFX", true)) {
            val soundId = soundMap[5]!!
            soundPool.play(soundId, 1f, 1f, 0, 0, 1f)
        }
    }

    private fun soundLose() {
        if (sharedPrefs.getBoolean("SFX", true)) {
            val soundId = soundMap[6]!!
            soundPool.play(soundId, 1f, 1f, 0, 0, 1f)
        }
    }

    private fun soundShot() {
        if (sharedPrefs.getBoolean("SHOT", true)) {
            val soundId = soundMap[1]!!
            soundPool.play(soundId, 1f, 1f, 0, 0, 1f)
        }
    }

    private fun spawnExplosion(x: Float, y: Float) {
        lifecycleScope.launch {
            val explosionView = LottieAnimationView(requireContext())
            val layoutParams = ViewGroup.LayoutParams(dpToPx(80), dpToPx(80))
            explosionView.layoutParams = layoutParams
            explosionView.setAnimation(R.raw.explosion_anim)
            explosionView.x = x
            explosionView.y = y
            binding.gameLayout.addView(explosionView)
            explosionView.playAnimation()
            delay(700)
            if (binding.gameLayout.contains(explosionView)) {
                binding.gameLayout.removeView(explosionView)
            }
        }
    }

    private fun setupPlayerView() {
        playerPlaneView = DraggableView.Builder(binding.playerPlane)
            .setListener(object : DraggableListener {
                override fun onPositionChanged(view: View) {
                    viewModel.setPlayerXY(x = view.x, y = view.y)
                }
            })
            .build()
        playerPlaneView.getView().x = viewModel.playerXY.value!!.first
        playerPlaneView.getView().y = viewModel.playerXY.value!!.second
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(
            this,
            GameViewModelFactory((xy.first - dpToPx(80)) to (xy.second - dpToPx(80)))
        )[GameViewModel::class.java]
    }

    private fun dpToPx(dp: Int): Int {
        val displayMetrics = resources.displayMetrics
        return (dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT)).roundToInt()
    }

    override fun onResume() {
        super.onResume()
        gameScope = CoroutineScope(Dispatchers.Main)
        if (viewModel.gameState) {
            Log.e("starr", "dd")
            generatePlayerAttack()
            generateEnemies()
            respawnEnemies()
            generateEnemyAttack()
            spawnSmallKit()
            spawnBigKit()
        } else {
            binding.playerPlane.visibility = View.GONE
            gameScope.cancel()
        }

    }

    override fun onPause() {
        super.onPause()
        gameScope.cancel()
    }
}