package io.github.lee0701.converter.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.*
import io.github.lee0701.converter.R
import io.github.lee0701.converter.databinding.ActivityUserDictionaryManagerBinding
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

        val wordListAdapter = WordListAdapter()
        binding.wordList.adapter = wordListAdapter
        binding.wordList.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.wordList.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        viewModel.dictionaries.observe(this, { list ->
            dictionaryListAdapter.clear()
            dictionaryListAdapter.addAll(list)
            val dictionary = list.firstOrNull()
            if(dictionary != null) viewModel.selectDictionary(dictionary)
        })

        viewModel.selectedDictionary.observe(this, { selected ->
            viewModel.loadAllWords(selected)
        })

        viewModel.words.observe(this, { list ->
            wordListAdapter.submitList(list)
        })

        viewModel.loadAllDictionaries()

        setContentView(binding.root)
    }

    override fun onItemSelected(adapterView: AdapterView<*>, view: View?, position: Int, p3: Long) {
        val dictionary = adapterView.adapter.getItem(position) as UserDictionary
        viewModel.selectDictionary(dictionary)
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        binding.dictionaryList.setSelection(0)
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

    class WordListAdapter: ListAdapter<UserDictionaryWord, WordViewHolder>(WordDiffCallback()) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
            return WordViewHolder(UserDictionaryWordListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
            holder.onBind(getItem(position))
        }
    }

    class WordViewHolder(
        private val binding: UserDictionaryWordListItemBinding
    ): RecyclerView.ViewHolder(binding.root) {
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