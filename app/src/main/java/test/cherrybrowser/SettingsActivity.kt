package test.cherrybrowser

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

/**
 * Minimal settings screen without relying on androidx.preference.
 * Stores settings to SharedPreferences.
 */
class SettingsActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("cherry_prefs", MODE_PRIVATE)
        setContentView(R.layout.activity_settings)
        val edtHomepage = findViewById<EditText>(R.id.edit_homepage)
        val spinnerSearch = findViewById<Spinner>(R.id.spinner_search)
        val switchSingleTab = findViewById<Switch>(R.id.switch_single_tab)
        val edtUserAgent = findViewById<EditText>(R.id.edit_user_agent)
        val btnSave = findViewById<Button>(R.id.btn_save)

        val engines = arrayOf("https://www.google.com/search?q=", "https://duckduckgo.com/?q=", "https://www.bing.com/search?q=")
        spinnerSearch.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, engines)

        edtHomepage.setText(prefs.getString("homepage", "https://www.example.com"))
        val ua = prefs.getString("user_agent_custom", "")
        edtUserAgent.setText(ua)
        switchSingleTab.isChecked = prefs.getBoolean("single_tab_mode", false)
        val sel = prefs.getString("search_engine", engines[0]) ?: engines[0]
        spinnerSearch.setSelection(engines.indexOf(sel).coerceAtLeast(0))

        btnSave.setOnClickListener {
            prefs.edit()
                .putString("homepage", edtHomepage.text.toString())
                .putBoolean("single_tab_mode", switchSingleTab.isChecked)
                .putString("user_agent_custom", edtUserAgent.text.toString())
                .putString("search_engine", spinnerSearch.selectedItem as String)
                .apply()
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
