package io.github.lee0701.converter.settings

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.*
import io.github.lee0701.converter.BuildConfig
import io.github.lee0701.converter.R
import io.github.lee0701.converter.databinding.ActivityUserDictionaryManagerBinding
import io.github.lee0701.converter.databinding.DialogUserDictionaryEditBinding
import io.github.lee0701.converter.databinding.DialogUserDictionaryWordEditBinding
import io.github.lee0701.converter.databinding.UserDictionaryWordListItemBinding
import io.github.lee0701.converter.userdictionary.UserDictionary
import io.github.lee0701.converter.userdictionary.UserDictionaryWord

class UserDictionaryManagerActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private lateinit var binding: ActivityUserDictionaryManagerBinding
    private val viewModel: UserDictionaryManagerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserDictionaryManagerBinding.inflate(layoutInflater)

        val dictionaryListAdapter = DictionaryListAdapter(this, mutableListOf())
        binding.dictionaryList.adapter = dictionaryListAdapter
        binding.dictionaryList.onItemSelectedListener = this

        val onItemClick = { word: UserDictionaryWord ->
            showEditWordDialog(word)
        }
        binding.addWord.setOnClickListener { _ ->
            val dictionaryId = viewModel.selectedDictionary.value?.id ?: return@setOnClickListener
            showEditWordDialog(UserDictionaryWord(dictionaryId, "", "", ""), true)
        }

        val wordListAdapter = WordListAdapter(onItemClick)
        binding.wordList.adapter = wordListAdapter
        binding.wordList.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.wordList.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        viewModel.dictionaries.observe(this, { list ->
            val previousSelected = viewModel.selectedDictionary.value?.id
            dictionaryListAdapter.clear()
            dictionaryListAdapter.addAll(list)
            val dictionary =
                if(previousSelected != null) list.find { it.id == previousSelected }
                else list.firstOrNull()
            if(dictionary != null) viewModel.selectDictionary(dictionary)
        })

        viewModel.selectedDictionary.observe(this, { _ ->
            viewModel.loadAllWords()
        })

        viewModel.words.observe(this, { list ->
            wordListAdapter.submitList(list)
            println(list)
        })

        viewModel.loadAllDictionaries()

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val resId =
            if(BuildConfig.IS_DONATION) R.menu.user_dictionary_manager_action_donation
            else R.menu.user_dictionary_manager_action
        menuInflater.inflate(resId, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.action_new_dictionary -> {
                showEditDictionaryDialog(UserDictionary(name = "", enabled = false), true)
                return true
            }
            R.id.action_edit_dictionary -> {
                showEditDictionaryDialog(viewModel.selectedDictionary.value ?: return false)
                return true
            }
            R.id.action_clear_dictionary -> {
                AlertDialog.Builder(this)
                    .setMessage(R.string.confirm_clear_dictionary)
                    .setPositiveButton(R.string.clear) { _, _ ->
                        val dictionary = viewModel.selectedDictionary.value ?: return@setPositiveButton
                        viewModel.clearDictionary(dictionary)
                    }.setNegativeButton(R.string.cancel) { _, _ ->
                    }.show()
            }
        }
        return false
    }

    override fun onItemSelected(adapterView: AdapterView<*>, view: View?, position: Int, p3: Long) {
        val dictionary = adapterView.adapter.getItem(position) as UserDictionary
        viewModel.selectDictionary(dictionary)
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        binding.dictionaryList.setSelection(0)
    }

    private fun showEditWordDialog(word: UserDictionaryWord, create: Boolean = false) {
        val view = DialogUserDictionaryWordEditBinding.inflate(layoutInflater)
        view.hangul.setText(word.hangul)
        view.hanja.setText(word.hanja)
        view.description.setText(word.description)
        val builder = AlertDialog.Builder(this).setView(view.root)
            .setPositiveButton(R.string.save) { _, _ ->
                val newWord = word.copy(
                    hangul = view.hangul.text.toString(),
                    hanja = view.hanja.text.toString(),
                    description = view.description.text.toString()
                )
                if(create) viewModel.insertWord(newWord)
                else viewModel.updateWord(word, newWord)
            }.setNegativeButton(R.string.cancel) { _, _ ->
            }
        if(!create) builder.setNeutralButton(R.string.delete) { _, _ ->
            viewModel.deleteWord(word)
        }
        builder.show()
    }

    private fun showEditDictionaryDialog(dictionary: UserDictionary, create: Boolean = false) {
        val view = DialogUserDictionaryEditBinding.inflate(layoutInflater)
        view.dictionaryName.setText(dictionary.name)
        view.enable.isChecked = dictionary.enabled
        val builder = AlertDialog.Builder(this).setView(view.root)
            .setPositiveButton(R.string.save) { _, _ ->
                val newDictionary = dictionary.copy(
                    name = view.dictionaryName.text.toString(),
                    enabled = view.enable.isChecked
                )
                if(create) viewModel.insertDictionary(newDictionary)
                else viewModel.updateDictionary(newDictionary)
            }.setNegativeButton(R.string.cancel) { _, _ ->
            }
        if(!create) builder.setNeutralButton(R.string.delete) { _, _ ->
            AlertDialog.Builder(this)
                .setMessage(R.string.confirm_delete_dictionary)
                .setPositiveButton(R.string.delete) { _, _ ->
                    viewModel.deleteDictionary(dictionary)
                }.setNegativeButton(R.string.cancel) { _, _ ->
                }.show()
        }
        builder.show()
    }

    class DictionaryListAdapter(
        context: Context, list: List<UserDictionary>
    ): ArrayAdapter<UserDictionary>(
        context, R.layout.user_dictionary_list_item, list
    ) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            return super.getView(position, convertView, parent).apply {
                val self = this as TextView
                self.text = getItem(position)?.name
            }
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            return super.getDropDownView(position, convertView, parent).apply {
                val self = this as TextView
                self.text = getItem(position)?.name
            }
        }
    }

    class WordListAdapter(
        private val onItemClick: (UserDictionaryWord) -> Unit
    ): ListAdapter<UserDictionaryWord, WordViewHolder>(WordDiffCallback()) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
            return WordViewHolder(UserDictionaryWordListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
            holder.onBind(getItem(position))
            holder.binding.root.setOnClickListener { onItemClick(getItem(position)) }
        }
    }

    class WordViewHolder(val binding: UserDictionaryWordListItemBinding): RecyclerView.ViewHolder(binding.root) {
        fun onBind(word: UserDictionaryWord) {
            binding.hangul.text = word.hangul
            binding.hanja.text = word.hanja
            binding.description.text = word.description
        }
    }

    class WordDiffCallback: DiffUtil.ItemCallback<UserDictionaryWord>() {
        override fun areItemsTheSame(oldItem: UserDictionaryWord, newItem: UserDictionaryWord): Boolean {
            return oldItem.dictionaryId == newItem.dictionaryId
                    && oldItem.hangul == newItem.hangul
                    && oldItem.hanja == newItem.hanja
        }

        override fun areContentsTheSame(oldItem: UserDictionaryWord, newItem: UserDictionaryWord): Boolean {
            return oldItem.description == newItem.description
        }
    }
}