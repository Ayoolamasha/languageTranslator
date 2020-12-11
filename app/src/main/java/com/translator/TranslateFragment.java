package com.translator;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
//import com.google.firebase.samples.apps.mlkit.translate.R;
//import com.google.firebase.samples.apps.mlkit.translate.java.TranslateViewModel.Language;
//import com.google.firebase.samples.apps.mlkit.translate.java.TranslateViewModel.ResultOrError;

import java.util.List;
import java.util.Objects;

import static android.content.Context.CLIPBOARD_SERVICE;

public class TranslateFragment extends Fragment {


    public static TranslateFragment newInstance() {
        return new TranslateFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.translate_fragment, container, false);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Button switchButton = view.findViewById(R.id.buttonSwitchLang);
        final ToggleButton sourceSyncButton = view.findViewById(R.id.buttonSyncSource);
        final ToggleButton targetSyncButton = view.findViewById(R.id.buttonSyncTarget);
        final TextInputEditText srcTextView = view.findViewById(R.id.sourceText);
        final TextView targetTextView = view.findViewById(R.id.translatedText);
        final TextView downloadedModelsTextView = view.findViewById(R.id.downloadedModels);
        final Spinner sourceLangSelector = view.findViewById(R.id.sourceLangSelector);
        final Spinner targetLangSelector = view.findViewById(R.id.targetLangSelector);
        final TranslateViewModel viewModel = new ViewModelProvider(this).get(TranslateViewModel.class);

        registerForContextMenu(targetTextView);

        // Get available language list and set up source and target language spinners
        // with default selections.
        final ArrayAdapter<TranslateViewModel.Language> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, viewModel.getAvailableLanguages());
        sourceLangSelector.setAdapter(adapter);
        targetLangSelector.setAdapter(adapter);
        sourceLangSelector.setSelection(adapter.getPosition(new TranslateViewModel.Language("en")));
        targetLangSelector.setSelection(adapter.getPosition(new TranslateViewModel.Language("es")));
        sourceLangSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setProgressText(targetTextView);
                viewModel.sourceLang.setValue(adapter.getItem(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                targetTextView.setText("");
            }
        });
        targetLangSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setProgressText(targetTextView);
                viewModel.targetLang.setValue(adapter.getItem(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                targetTextView.setText("");
            }
        });

        switchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setProgressText(targetTextView);
                int sourceLangPosition = sourceLangSelector.getSelectedItemPosition();
                sourceLangSelector.setSelection(targetLangSelector.getSelectedItemPosition());
                targetLangSelector.setSelection(sourceLangPosition);
            }
        });

        // Set up toggle buttons to delete or download remote models locally.
        sourceSyncButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                TranslateViewModel.Language language = adapter.getItem(sourceLangSelector.getSelectedItemPosition());
                if (isChecked) {
                    viewModel.downloadLanguage(language);
                } else {
                    viewModel.deleteLanguage(language);
                }
            }
        });
        targetSyncButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                TranslateViewModel.Language language = adapter.getItem(targetLangSelector.getSelectedItemPosition());
                if (isChecked) {
                    viewModel.downloadLanguage(language);
                } else {
                    viewModel.deleteLanguage(language);
                }
            }
        });

        // Translate input text as it is typed
        srcTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                setProgressText(targetTextView);
                viewModel.sourceText.postValue(s.toString());
            }
        });
        viewModel.translatedText.observe(getViewLifecycleOwner(), new Observer<TranslateViewModel.ResultOrError>() {
            @Override
            public void onChanged(TranslateViewModel.ResultOrError resultOrError) {
                if (resultOrError.error != null) {
                    srcTextView.setError(resultOrError.error.getLocalizedMessage());
                } else {
                    targetTextView.setText(resultOrError.result);
                }
            }
        });

        // Update sync toggle button states based on downloaded models list.
        viewModel.availableModels.observe(getViewLifecycleOwner(), new Observer<List<String>>() {
            @Override
            public void onChanged(@Nullable List<String> firebaseTranslateRemoteModels) {
                String output = getContext().getString(R.string.downloaded_models_label,
                        firebaseTranslateRemoteModels);
                downloadedModelsTextView.setText(output);
                sourceSyncButton.setChecked(firebaseTranslateRemoteModels.contains(
                        adapter.getItem(sourceLangSelector.getSelectedItemPosition()).getCode()));
                targetSyncButton.setChecked(firebaseTranslateRemoteModels.contains(
                        adapter.getItem(targetLangSelector.getSelectedItemPosition()).getCode()));
            }
        });
    }

    private void setProgressText(TextView tv) {
        tv.setText(getContext().getString(R.string.translate_progress));
    }

    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, @Nullable ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        //menu.setHeaderTitle("Options");
        switch (v.getId()){
            case R.id.translatedText:
                menu.add(0, v.getId(),0, "Copy");
                menu.add(0, v.getId(),0, "Copy To Clipboard");
                TextView textView = (TextView) v;
                ClipboardManager manager = (ClipboardManager) Objects.requireNonNull(getActivity()).getSystemService(CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText("text", textView.getText());
                if (manager != null){
                    manager.setPrimaryClip(clipData);
                }
                break;
        }
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()){
            case R.id.translatedText:
                Toast.makeText(getActivity(), "Translation Copied", Toast.LENGTH_SHORT).show();
                break;
        }

        return super.onContextItemSelected(item);



    }
}

