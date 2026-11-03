package com.sukaina.tube.ui.parent

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sukaina.tube.R
import com.sukaina.tube.SukainaApp
import com.sukaina.tube.databinding.ActivityOnboardingBinding
import com.sukaina.tube.ui.MainActivity

/**
 * معالج الإعداد الأول — يظهر عند أول تشغيل فقط.
 * 3 خطوات: ترحيب → إعداد رمز PIN (اختياري) → ابدأ المشاهدة.
 */
class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private val app get() = application as SukainaApp
    private var step = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        renderStep()
        binding.btnPrimary.setOnClickListener { handlePrimary() }
        binding.btnSecondary.setOnClickListener { handleSecondary() }
    }

    private fun renderStep() {
        when (step) {
            0 -> {
                binding.stepTitle.text = "أهلًا بك في Sukaina Tube"
                binding.stepDesc.text = "تطبيق يعرض فيديوهات هاتفك بواجهة تشبه يوتيوب.\nبدون إنترنت، بدون إعلانات، آمن للأطفال."
                binding.btnPrimary.text = "ابدأ الإعداد"
                binding.btnSecondary.text = "تخطّي مؤقتًا"
                binding.dot1.setBackgroundResource(R.drawable.dot_active)
                binding.dot2.setBackgroundResource(R.drawable.dot_inactive)
                binding.dot3.setBackgroundResource(R.drawable.dot_inactive)
            }
            1 -> {
                binding.stepTitle.text = "إعداد رمز ولي الأمر"
                binding.stepDesc.text = "يمكنك حماية الإعدادات برمز سري من 4 أرقام.\nيُفتح بالضغط المطوّل على الشعار."
                binding.btnPrimary.text = "إنشاء رمز الآن"
                binding.btnSecondary.text = "لاحقًا"
                binding.dot1.setBackgroundResource(R.drawable.dot_inactive)
                binding.dot2.setBackgroundResource(R.drawable.dot_active)
                binding.dot3.setBackgroundResource(R.drawable.dot_inactive)
            }
            2 -> {
                binding.stepTitle.text = "كل شيء جاهز!"
                binding.stepDesc.text = "اختر المجلدات من إعدادات ولي الأمر.\nاستمتع بمشاهدة آمنة لطفلك."
                binding.btnPrimary.text = "ابدأ المشاهدة"
                binding.btnSecondary.text = "افتح الإعدادات"
                binding.dot1.setBackgroundResource(R.drawable.dot_inactive)
                binding.dot2.setBackgroundResource(R.drawable.dot_inactive)
                binding.dot3.setBackgroundResource(R.drawable.dot_active)
            }
        }
    }

    private fun handlePrimary() {
        when (step) {
            0 -> { step = 1; renderStep() }
            1 -> {
                // فتح شاشة إنشاء PIN مباشرة
                val intent = Intent(this, ParentPinActivity::class.java)
                intent.putExtra(ParentPinActivity.EXTRA_CHANGE, true)
                startActivityForResult(intent, REQ_PIN)
            }
            2 -> finishOnboarding()
        }
    }

    private fun handleSecondary() {
        when (step) {
            0 -> finishOnboarding()
            1 -> { step = 2; renderStep() }
            2 -> {
                // فتح إعدادات ولي الأمر مباشرة
                val intent = Intent(this, ParentSettingsActivity::class.java)
                startActivity(intent)
                finishOnboarding()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_PIN) {
            step = 2
            renderStep()
        }
    }

    private fun finishOnboarding() {
        app.settings.onboardingCompleted = true
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    companion object {
        private const val REQ_PIN = 100
    }
}
