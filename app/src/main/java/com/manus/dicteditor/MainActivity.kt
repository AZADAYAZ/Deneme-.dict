package com.manus.dicteditor

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.manus.dicteditor.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val words = mutableListOf<Word>()
    private lateinit var adapter: WordAdapter
    private val parser = BinaryDictParser()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = WordAdapter(words)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.btnLoad.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            startActivityForResult(intent, 100)
        }

        binding.btnSave.setOnClickListener {
            saveDictionary()
        }

        binding.fabAdd.setOnClickListener {
            showAddWordDialog()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                100 -> data?.data?.let { loadDictionary(it) }
                200 -> data?.data?.let { saveToUri(it) }
            }
        }
    }

    private fun saveToUri(uri: Uri) {
        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                AOSPEncoder().encodeV2(words, outputStream)
                Toast.makeText(this, "Sözlük kaydedildi", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Kaydetme hatası: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadDictionary(uri: Uri) {
        Thread {
            try {
                val file = File(cacheDir, "temp.dict")
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(file).use { output -> input.copyTo(output) }
                } ?: throw IllegalArgumentException("Dosya açılamadı")

                val parsedWords = parser.parse(file)
                val parserError = parser.lastError
                runOnUiThread {
                    if (parserError != null) {
                        Toast.makeText(
                            this,
                            "Sözlük çözümlenemedi: $parserError",
                            Toast.LENGTH_LONG
                        ).show()
                        return@runOnUiThread
                    }
                    words.clear()
                    words.addAll(parsedWords)
                    adapter.notifyDataSetChanged()
                    Toast.makeText(
                        this,
                        "${words.size} kelime yüklendi",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (error: Throwable) {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Dosya yükleme hatası: ${error.message ?: error.javaClass.simpleName}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    private fun saveDictionary() {
        if (words.isEmpty()) return
        
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "application/octet-stream"
        intent.putExtra(Intent.EXTRA_TITLE, "new_dictionary.dict")
        startActivityForResult(intent, 200)
    }

    private fun showAddWordDialog() {
        val view = LayoutInflater.from(this).inflate(android.R.layout.two_line_list_item, null)
        // Note: Simplified for this example, in real app use custom layout
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Yeni Kelime Ekle")
        val input = EditText(this)
        input.hint = "Kelime,Frekans (örn: merhaba,255)"
        builder.setView(input)
        builder.setPositiveButton("Ekle") { _, _ ->
            val text = input.text.toString()
            if (text.contains(",")) {
                val parts = text.split(",")
                words.add(Word(parts[0], parts[1].toIntOrNull() ?: 100))
                adapter.notifyDataSetChanged()
            }
        }
        builder.show()
    }

    inner class WordAdapter(private val items: List<Word>) : RecyclerView.Adapter<WordAdapter.ViewHolder>() {
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvWord: TextView = view.findViewById(R.id.tvWord)
            val tvFreq: TextView = view.findViewById(R.id.tvFreq)
            val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_word, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvWord.text = item.text
            holder.tvFreq.text = item.frequency.toString()
            holder.btnDelete.setOnClickListener {
                words.removeAt(position)
                notifyDataSetChanged()
            }
        }

        override fun getItemCount() = items.size
    }
}
