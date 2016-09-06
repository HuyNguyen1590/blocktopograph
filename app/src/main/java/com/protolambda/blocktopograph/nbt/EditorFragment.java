package com.protolambda.blocktopograph.nbt;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.text.Editable;
import android.text.TextWatcher;
import com.protolambda.blocktopograph.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.protolambda.blocktopograph.R;
import com.protolambda.blocktopograph.WorldActivity;
import com.protolambda.blocktopograph.WorldActivityInterface;
import com.protolambda.blocktopograph.nbt.convert.NBTConstants;
import com.protolambda.blocktopograph.nbt.tags.*;
import com.unnamed.b.atv.model.TreeNode;
import com.unnamed.b.atv.view.AndroidTreeView;

import java.util.ArrayList;
import java.util.List;

public class EditorFragment extends Fragment {

    /**
     *
     * TODO:
     *
     * - The onSomethingChanged listeners should start Asynchronous tasks
     *     when directly modifying NBT.
     *
     * - This editor should be refactored into parts, it grew too large.
     *
     * - The functions lack documentation. Add it. Ask @protolambda for now...
     *
     */

    private EditableNBT nbt;

    public void setEditableNBT(EditableNBT nbt){
        this.nbt = nbt;
    }

    public static class ChainTag {

        public Tag parent, self;

        public ChainTag(Tag parent, Tag self){
            this.parent = parent;
            this.self = self;
        }
    }

    public static class RootNodeHolder extends TreeNode.BaseNodeViewHolder<EditableNBT>{


        public RootNodeHolder(Context context) {
            super(context);
        }

        @Override
        public View createNodeView(TreeNode node, EditableNBT value) {

            final LayoutInflater inflater = LayoutInflater.from(context);

            final View tagView = inflater.inflate(R.layout.tag_root_layout, null, false);
            TextView tagName = (TextView) tagView.findViewById(R.id.tag_name);
            tagName.setText(value.getRootTitle());

            return tagView;
        }

        @Override
        public void toggle(boolean active) {
        }

        @Override
        public int getContainerStyle() {
            return R.style.TreeNodeStyleCustom;
        }
    }


    public static class NBTNodeHolder extends TreeNode.BaseNodeViewHolder<ChainTag> {

        private final EditableNBT nbt;

        public NBTNodeHolder(EditableNBT nbt, Context context) {
            super(context);
            this.nbt = nbt;
        }

        @SuppressLint("SetTextI18n")
        @Override
        public View createNodeView(TreeNode node, final ChainTag chain) {

            if(chain == null) return null;
            Tag tag = chain.self;
            if(tag == null) return null;

            final LayoutInflater inflater = LayoutInflater.from(context);

            int layoutID;

            switch (tag.getType()) {
                case COMPOUND: {
                    List<Tag> value = ((CompoundTag) tag).getValue();
                    if (value != null) {
                        for (Tag child : value) {
                            node.addChild(new TreeNode(new ChainTag(tag, child)).setViewHolder(new NBTNodeHolder(nbt, context)));
                        }
                    }

                    layoutID = R.layout.tag_compound_layout;
                    break;
                }
                case LIST: {
                    List<Tag> value = ((ListTag) tag).getValue();

                    if (value != null) {
                        for (Tag child : value) {
                            node.addChild(new TreeNode(new ChainTag(tag, child)).setViewHolder(new NBTNodeHolder(nbt, context)));
                        }
                    }

                    layoutID = R.layout.tag_list_layout;
                    break;
                }
                case BYTE_ARRAY: {
                    layoutID = R.layout.tag_default_layout;
                    break;
                }
                case BYTE: {
                    String name = tag.getName().toLowerCase();

                    //TODO differentiate boolean tags from byte tags better
                    if (name.startsWith("has") || name.startsWith("is")) {
                        layoutID = R.layout.tag_boolean_layout;
                    } else {
                        layoutID = R.layout.tag_byte_layout;
                    }
                    break;
                }
                case SHORT:
                    layoutID = R.layout.tag_short_layout;
                    break;
                case INT:
                    layoutID = R.layout.tag_int_layout;
                    break;
                case LONG:
                    layoutID = R.layout.tag_long_layout;
                    break;
                case FLOAT:
                    layoutID = R.layout.tag_float_layout;
                    break;
                case DOUBLE:
                    layoutID = R.layout.tag_double_layout;
                    break;
                case STRING:
                    layoutID = R.layout.tag_string_layout;
                    break;
                default:
                    layoutID = R.layout.tag_default_layout;
                    break;
            }

            final View tagView = inflater.inflate(layoutID, null, false);
            TextView tagName = (TextView) tagView.findViewById(R.id.tag_name);
            tagName.setText(tag.getName());

            switch (layoutID){
                case R.layout.tag_boolean_layout: {
                    final CheckBox checkBox = (CheckBox) tagView.findViewById(R.id.checkBox);
                    final ByteTag byteTag = (ByteTag) tag;
                    checkBox.setChecked(byteTag.getValue() == (byte) 1);
                    checkBox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
                        /**
                         * Called when the checked state of a compound button has changed.
                         *
                         * @param buttonView The compound button view whose state has changed.
                         * @param isChecked  The new checked state of buttonView.
                         */
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            byteTag.setValue(isChecked ? (byte) 1 : (byte) 0);
                            nbt.setModified();
                        }
                    });
                    break;
                }
                case R.layout.tag_byte_layout: {
                    final EditText editText = (EditText) tagView.findViewById(R.id.byteField);
                    final ByteTag byteTag = (ByteTag) tag;
                    //parse the byte as an unsigned byte
                    editText.setText(""+(((int) byteTag.getValue()) & 0xFF));
                    editText.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) { }

                        @Override
                        public void afterTextChanged(Editable s) {
                            String sValue = s.toString();
                            try {
                                byteTag.setValue(Byte.valueOf(sValue));
                                nbt.setModified();
                            } catch (NumberFormatException e){
                                Snackbar.make(editText, "\"" + sValue + "\" is invalid!", Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                            }
                        }
                    });
                    break;
                }
                case R.layout.tag_short_layout: {
                    final EditText editText = (EditText) tagView.findViewById(R.id.shortField);
                    final ShortTag shortTag = (ShortTag) tag;
                    editText.setText(shortTag.getValue().toString());
                    editText.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) { }

                        @Override
                        public void afterTextChanged(Editable s) {
                            String sValue = s.toString();
                            try {
                                shortTag.setValue(Short.valueOf(sValue));
                                nbt.setModified();
                            } catch (NumberFormatException e){
                                Snackbar.make(editText, "\"" + sValue + "\" is invalid!", Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                            }
                        }
                    });
                    break;
                }
                case R.layout.tag_int_layout: {
                    final EditText editText = (EditText) tagView.findViewById(R.id.intField);
                    final IntTag intTag = (IntTag) tag;
                    editText.setText(intTag.getValue().toString());
                    editText.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) { }

                        @Override
                        public void afterTextChanged(Editable s) {
                            String sValue = s.toString();
                            try {
                                intTag.setValue(Integer.valueOf(sValue));
                                nbt.setModified();
                            } catch (NumberFormatException e){
                                Snackbar.make(editText, "\"" + sValue + "\" is invalid!", Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                            }
                        }
                    });
                    break;
                }
                case R.layout.tag_long_layout: {
                    final EditText editText = (EditText) tagView.findViewById(R.id.longField);
                    final LongTag longTag = (LongTag) tag;
                    editText.setText(longTag.getValue().toString());
                    editText.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) { }

                        @Override
                        public void afterTextChanged(Editable s) {
                            String sValue = s.toString();
                            try {
                                longTag.setValue(Long.valueOf(sValue));
                                nbt.setModified();
                            } catch (NumberFormatException e){
                                Snackbar.make(editText, "\"" + sValue + "\" is invalid!", Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                            }
                        }
                    });
                    break;
                }
                case R.layout.tag_float_layout: {
                    final EditText editText = (EditText) tagView.findViewById(R.id.floatField);
                    final FloatTag floatTag = (FloatTag) tag;
                    editText.setText(floatTag.getValue().toString());
                    editText.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) { }

                        @Override
                        public void afterTextChanged(Editable s) {
                            String sValue = s.toString();
                            try {
                                floatTag.setValue(Float.valueOf(sValue));
                                nbt.setModified();
                            } catch (NumberFormatException e){
                                Snackbar.make(editText, "\"" + sValue + "\" is invalid!", Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                            }
                        }
                    });
                    break;
                }
                case R.layout.tag_double_layout: {
                    final EditText editText = (EditText) tagView.findViewById(R.id.doubleField);
                    final DoubleTag doubleTag = (DoubleTag) tag;
                    editText.setText(doubleTag.getValue().toString());
                    editText.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) { }

                        @Override
                        public void afterTextChanged(Editable s) {
                            String sValue = s.toString();
                            try {
                                doubleTag.setValue(Double.valueOf(sValue));
                                nbt.setModified();
                            } catch (NumberFormatException e){
                                Snackbar.make(editText, "\"" + sValue + "\" is invalid!", Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                            }
                        }
                    });
                    break;
                }
                case R.layout.tag_string_layout: {
                    final EditText editText = (EditText) tagView.findViewById(R.id.stringField);
                    final StringTag stringTag = (StringTag) tag;
                    editText.setText(stringTag.getValue());
                    editText.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) { }

                        @Override
                        public void afterTextChanged(Editable s) {
                            nbt.setModified();
                            stringTag.setValue(s.toString());
                        }
                    });
                    break;
                }
                default:
                    break;

            }

            return tagView;
        }

        @Override
        public void toggle(boolean active) {
        }

        @Override
        public int getContainerStyle() {
            return R.style.TreeNodeStyleCustom;
        }

    }

    public enum NBTEditOption {

        CANCEL("Cancel"),
        COPY("Copy"),
        PASTE_OVERWRITE("Paste (overwrite)"),
        PASTE_SUBTAG("Paste (as sub-tag)"),
        DELETE("Delete"),
        RENAME("Rename"),
        ADD_SUBTAG("Add sub-tag");

        public final String name;

        NBTEditOption(String name){
            this.name = name;
        }

        public static String[] options;

        static {
            NBTEditOption[] values = values();
            int len = values.length;
            options = new String[len];
            for(int i = 0; i < len; i++){
                options[i] = values[i].name;
            }
        }
    }

    public enum RootNBTEditOption {

        ADD_NBT_TAG("Add NBT tag"),
        PASTE_SUB_TAG("Paste as sub-tag"),
        REMOVE_ALL_TAGS("Remove all NBT tags");

        public final String name;

        RootNBTEditOption(String name){
            this.name = name;
        }

        public static String[] options;

        static {
            RootNBTEditOption[] values = values();
            int len = values.length;
            options = new String[len];
            for(int i = 0; i < len; i++){
                options[i] = values[i].name;
            }
        }
    }



    public static Tag clipboard;


    //returns true if there is a tag in content with a name equals to key.
    boolean checkKeyCollision(String key, List<Tag> content){
        if(content == null || content.isEmpty()) return false;
        if(key == null) key = "";
        String tagName;
        for(Tag tag : content) {
            tagName = tag.getName();
            if(tagName == null) tagName = "";
            if(tagName.equals(key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        if(nbt == null){
            new Exception("No NBT data provided").printStackTrace();
            getActivity().finish();

            return null;
        }

        final View rootView = inflater.inflate(R.layout.nbt_editor, container, false);

        // edit functionality
        // ================================

        TreeNode superRoot = TreeNode.root();
        TreeNode root = new TreeNode(nbt);
        superRoot.addChild(root);
        root.setExpanded(true);
        root.setSelectable(false);


        final Activity activity = getActivity();

        root.setViewHolder(new RootNodeHolder(activity));

        for(Tag tag : nbt.getTags()){
            root.addChild(new TreeNode(new ChainTag(null, tag)).setViewHolder(new NBTNodeHolder(nbt, activity)));
        }

        FrameLayout frame = (FrameLayout) rootView.findViewById(R.id.nbt_editor_frame);
        final AndroidTreeView tree = new AndroidTreeView(getActivity(), superRoot);
        tree.setUse2dScroll(true);

        final View treeView = tree.getView();

        tree.setDefaultNodeLongClickListener(new TreeNode.TreeNodeLongClickListener() {
            @Override
            public boolean onLongClick(final TreeNode node, final Object value) {

                Log.d("NBT editor: Long click!");


                //root tag has nbt as value
                if(value instanceof EditableNBT){

                    if(!nbt.enableRootModifications){
                        Toast.makeText(activity, "Cannot edit root NBT tag.", Toast.LENGTH_LONG).show();
                        return true;
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);

                    builder.setTitle("Root NBT options")
                            .setItems(RootNBTEditOption.options, new DialogInterface.OnClickListener() {

                                private void showMsg(String msg){
                                    Toast.makeText(activity, msg, Toast.LENGTH_LONG).show();
                                }

                                public void onClick(DialogInterface dialog, int which) {
                                    try {
                                        RootNBTEditOption option = RootNBTEditOption.values()[which];

                                        switch (option){
                                            case ADD_NBT_TAG:{
                                                final EditText nameText = new EditText(activity);
                                                nameText.setHint("Tag name here...");

                                                //NBT tag type spinner
                                                final Spinner spinner = new Spinner(activity);
                                                ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(
                                                        activity, android.R.layout.simple_spinner_item, NBTConstants.NBTType.editorOptions_asString);

                                                spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                                spinner.setAdapter(spinnerArrayAdapter);

                                                //wrap editText and spinner in linear layout
                                                LinearLayout linearLayout = new LinearLayout(activity);
                                                linearLayout.setOrientation(LinearLayout.VERTICAL);
                                                linearLayout.setLayoutParams(
                                                        new LinearLayout.LayoutParams(
                                                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                                                Gravity.BOTTOM));
                                                linearLayout.addView(nameText);
                                                linearLayout.addView(spinner);

                                                //wrap layout in alert
                                                AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                                                alert.setTitle("Create NBT tag");
                                                alert.setView(linearLayout);

                                                //alert can create a new tag
                                                alert.setPositiveButton("Create", new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int whichButton) {

                                                        //new tag name
                                                        Editable newNameEditable = nameText.getText();
                                                        String newName = (newNameEditable == null || newNameEditable.toString().equals("")) ? null : newNameEditable.toString();

                                                        //new tag type
                                                        int spinnerIndex = spinner.getSelectedItemPosition();
                                                        NBTConstants.NBTType nbtType = NBTConstants.NBTType.editorOptions_asType[spinnerIndex];

                                                        //create new tag
                                                        Tag newTag = NBTConstants.NBTType.newInstance(newName, nbtType);

                                                        //add tag to nbt
                                                        nbt.addRootTag(newTag);
                                                        tree.addNode(node, new TreeNode(new ChainTag(null, newTag)).setViewHolder(new NBTNodeHolder(nbt, activity)));

                                                        nbt.setModified();

                                                    }
                                                });

                                                //or alert is cancelled
                                                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int whichButton) {
                                                        Log.d("NBT tag creation cancelled");
                                                    }
                                                });

                                                alert.show();

                                                return;
                                            }
                                            case PASTE_SUB_TAG: {
                                                if(clipboard == null){
                                                    showMsg("Clipboard is empty!");
                                                    return;
                                                }

                                                Tag copy = clipboard.getDeepCopy();
                                                nbt.addRootTag(copy);
                                                tree.addNode(node, new TreeNode(new ChainTag(null, copy)).setViewHolder(new NBTNodeHolder(nbt, activity)));
                                                nbt.setModified();

                                                return;
                                            }
                                            case REMOVE_ALL_TAGS:{

                                                //wrap layout in alert
                                                AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                                                alert.setTitle("Do you really want to delete all NBT tags?");

                                                //alert can create a new tag
                                                alert.setPositiveButton("DELETE!", new DialogInterface.OnClickListener() {

                                                    public void onClick(DialogInterface dialog, int whichButton) {

                                                        List<TreeNode> children = new ArrayList<>(node.getChildren());
                                                        //new tag name
                                                        for(TreeNode child : children){
                                                            tree.removeNode(child);
                                                            Object childValue = child.getValue();
                                                            if(childValue != null && childValue instanceof ChainTag)
                                                                nbt.removeRootTag(((ChainTag) childValue).self);
                                                        }
                                                        nbt.setModified();
                                                    }

                                                });

                                                //or alert is cancelled
                                                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int whichButton) {
                                                        Log.d("NBT tag creation cancelled");
                                                    }
                                                });

                                                alert.show();

                                                break;
                                            }
                                            default: {
                                                Log.d("User clicked unknown NBTEditOption! "+option.name());
                                            }
                                        }
                                    } catch (Exception e){
                                        showMsg("Error: failed to do NBT change.");
                                    }
                                }

                            });
                    builder.show();
                    return true;


                } else if(value instanceof ChainTag){
                    //other tags have a chain-tag as value


                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);

                    builder.setTitle("NBT tag options")
                            .setItems(NBTEditOption.options, new DialogInterface.OnClickListener() {

                                private void showMsg(String msg){
                                    Toast.makeText(activity, msg, Toast.LENGTH_LONG).show();
                                }

                                @SuppressWarnings("unchecked")
                                public void onClick(DialogInterface dialog, int which) {
                                    try{
                                        NBTEditOption editOption = NBTEditOption.values()[which];

                                        final Tag parent = ((ChainTag) value).parent;
                                        final Tag self = ((ChainTag) value).self;

                                        if(self == null) return;//WTF?

                                        if(editOption == null) return;//WTF?

                                        switch (editOption){
                                            case CANCEL: {
                                                return;
                                            }
                                            case COPY: {
                                                clipboard = self.getDeepCopy();
                                                return;
                                            }
                                            case PASTE_OVERWRITE: {
                                                if(clipboard == null){
                                                    showMsg("Clipboard is empty!");
                                                    return;
                                                }

                                                if(parent == null){
                                                    //it is one of the children of the root node
                                                    nbt.removeRootTag(self);
                                                    Tag copy = clipboard.getDeepCopy();
                                                    nbt.addRootTag(copy);
                                                    tree.addNode(node.getParent(), new TreeNode(new ChainTag(null, copy)).setViewHolder(new NBTNodeHolder(nbt, activity)));
                                                    tree.removeNode(node);
                                                    nbt.setModified();
                                                    return;
                                                } else {

                                                    ArrayList<Tag> content;
                                                    switch (parent.getType()) {
                                                        case LIST: {
                                                            content = ((ListTag) parent).getValue();
                                                            break;
                                                        }
                                                        case COMPOUND: {
                                                            content = ((CompoundTag) parent).getValue();
                                                            if(checkKeyCollision(clipboard.getName(), content)){
                                                                showMsg("Error: clipboard-key already exists in the compound!");
                                                                return;
                                                            }
                                                            break;
                                                        }
                                                        default: {
                                                            showMsg("Error: cannot overwrite tag: unknown parent NBT type.");
                                                            return;
                                                        }
                                                    }
                                                    if(content != null){
                                                        content.remove(self);
                                                        Tag copy = clipboard.getDeepCopy();
                                                        content.add(copy);
                                                        tree.addNode(node.getParent(), new TreeNode(new ChainTag(parent, copy)).setViewHolder(new NBTNodeHolder(nbt, activity)));
                                                        tree.removeNode(node);
                                                        nbt.setModified();
                                                        return;
                                                    }
                                                    else showMsg("Error: cannot overwrite NBT in an empty parent tag.");
                                                    return;
                                                }
                                            }
                                            case PASTE_SUBTAG: {
                                                if(clipboard == null){
                                                    showMsg("Clipboard is empty!");
                                                    return;
                                                }

                                                ArrayList<Tag> content;
                                                switch (self.getType()) {
                                                    case LIST: {
                                                        content = ((ListTag) self).getValue();
                                                        break;
                                                    }
                                                    case COMPOUND: {
                                                        content = ((CompoundTag) self).getValue();
                                                        if(checkKeyCollision(clipboard.getName(), content)){
                                                            showMsg("Error: clipboard-key already exists in the compound!");
                                                            return;
                                                        }
                                                        break;
                                                    }
                                                    default: {
                                                        showMsg("Error: cannot paste as sub-tag: unknown parent NBT type.");
                                                        return;
                                                    }
                                                }
                                                if(content == null){
                                                    content = new ArrayList<>();
                                                    self.setValue(content);
                                                }

                                                Tag copy = clipboard.getDeepCopy();
                                                content.add(copy);
                                                tree.addNode(node, new TreeNode(new ChainTag(self, copy)).setViewHolder(new NBTNodeHolder(nbt, activity)));
                                                nbt.setModified();

                                                return;
                                            }
                                            case DELETE: {
                                                if(parent == null){
                                                    //it is one of the children of the root node
                                                    tree.removeNode(node);
                                                    nbt.removeRootTag(self);
                                                    nbt.setModified();
                                                    return;
                                                }

                                                ArrayList<Tag> content;
                                                switch (parent.getType()){
                                                    case LIST:{
                                                        content = ((ListTag) parent).getValue();
                                                        break;
                                                    }
                                                    case COMPOUND:{
                                                        content = ((CompoundTag) parent).getValue();
                                                        break;
                                                    }
                                                    default:{
                                                        showMsg("Error: cannot overwrite tag: unknown parent NBT type.");
                                                        return;
                                                    }
                                                }
                                                if(content != null){
                                                    content.remove(self);
                                                    tree.removeNode(node);
                                                    nbt.setModified();
                                                }
                                                else showMsg("Error: cannot remove NBT from empty list.");
                                                return;
                                            }
                                            case RENAME: {
                                                final EditText edittext = new EditText(activity);
                                                edittext.setHint("Tag name here...");

                                                AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                                                alert.setTitle("Rename NBT tag");

                                                alert.setView(edittext);

                                                alert.setPositiveButton("Rename", new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int whichButton) {
                                                        Editable newNameEditable = edittext.getText();
                                                        String newName = (newNameEditable == null || newNameEditable.toString().equals("")) ? null : newNameEditable.toString();

                                                        if(parent != null
                                                                && parent instanceof CompoundTag
                                                                && checkKeyCollision(newName, ((CompoundTag) parent).getValue())){
                                                            showMsg("Error: parent-NBT-tag already contains that key!");
                                                            return;
                                                        }

                                                        self.setName(newName);

                                                        //refresh view with new TreeNode
                                                        tree.addNode(node.getParent(), new TreeNode(new ChainTag(parent, self)).setViewHolder(new NBTNodeHolder(nbt, activity)));
                                                        tree.removeNode(node);

                                                        nbt.setModified();
                                                    }
                                                });

                                                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int whichButton) {
                                                        Log.d("Cancelled rename NBT tag");
                                                    }
                                                });

                                                alert.show();

                                                return;
                                            }
                                            case ADD_SUBTAG: {
                                                switch (self.getType()){
                                                    case LIST:
                                                    case COMPOUND:{
                                                        final EditText nameText = new EditText(activity);
                                                        nameText.setHint("Tag name here...");

                                                        //NBT tag type spinner
                                                        final Spinner spinner = new Spinner(activity);
                                                        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(
                                                                activity, android.R.layout.simple_spinner_item, NBTConstants.NBTType.editorOptions_asString);

                                                        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                                        spinner.setAdapter(spinnerArrayAdapter);

                                                        //wrap editText and spinner in linear layout
                                                        LinearLayout linearLayout = new LinearLayout(activity);
                                                        linearLayout.setOrientation(LinearLayout.VERTICAL);
                                                        linearLayout.setLayoutParams(
                                                                new LinearLayout.LayoutParams(
                                                                        LinearLayout.LayoutParams.WRAP_CONTENT,
                                                                        LinearLayout.LayoutParams.WRAP_CONTENT,
                                                                        Gravity.BOTTOM));
                                                        linearLayout.addView(nameText);
                                                        linearLayout.addView(spinner);

                                                        //wrap layout in alert
                                                        AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                                                        alert.setTitle("Create NBT tag");
                                                        alert.setView(linearLayout);

                                                        //alert can create a new tag
                                                        alert.setPositiveButton("Create", new DialogInterface.OnClickListener() {
                                                            public void onClick(DialogInterface dialog, int whichButton) {

                                                                //new tag name
                                                                Editable newNameEditable = nameText.getText();
                                                                String newName = (newNameEditable == null || newNameEditable.toString().equals("")) ? null : newNameEditable.toString();

                                                                //new tag type
                                                                int spinnerIndex = spinner.getSelectedItemPosition();
                                                                NBTConstants.NBTType nbtType = NBTConstants.NBTType.editorOptions_asType[spinnerIndex];


                                                                ArrayList<Tag> content;
                                                                if(self instanceof CompoundTag) {

                                                                    content = ((CompoundTag) self).getValue();

                                                                    if(checkKeyCollision(newName, content)){
                                                                        showMsg("Error: that key already exists in the compound!");
                                                                        return;
                                                                    }
                                                                }
                                                                else if(self instanceof ListTag){
                                                                    content = ((ListTag) self).getValue();
                                                                }
                                                                else return;//WTF?

                                                                if(content == null){
                                                                    content = new ArrayList<>();
                                                                    self.setValue(content);
                                                                }

                                                                //create new tag
                                                                Tag newTag = NBTConstants.NBTType.newInstance(newName, nbtType);

                                                                //add tag to nbt
                                                                content.add(newTag);
                                                                tree.addNode(node, new TreeNode(new ChainTag(self, newTag)).setViewHolder(new NBTNodeHolder(nbt, activity)));

                                                                nbt.setModified();

                                                            }
                                                        });

                                                        //or alert is cancelled
                                                        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                                Log.d("NBT tag creation cancelled");
                                                            }
                                                        });

                                                        alert.show();

                                                        return;

                                                    }
                                                    default:{
                                                        showMsg("Error: sub-tags can only be added to compound tags and list tags.");
                                                        return;
                                                    }
                                                }
                                            }
                                            default: {
                                                Log.d("User clicked unknown NBTEditOption! "+editOption.name());
                                            }

                                        }
                                    } catch (Exception e) {
                                        showMsg("Error: failed to do NBT change.");
                                    }

                                }
                            });

                    builder.show();
                    return true;
                }
                return false;
            }
        });
        frame.addView(treeView, 0);



        // save functionality
        // ================================

        FloatingActionButton fabSaveNBT = (FloatingActionButton) rootView.findViewById(R.id.fab_save_nbt);
        assert fabSaveNBT != null;
        fabSaveNBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                if(!nbt.isModified()){
                    Snackbar.make(view, "No data has changed, nothing to save.", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                } else {
                    new AlertDialog.Builder(activity)
                            .setTitle("NBT editor")
                            .setMessage("Do you really want to save your changes?")
                            .setIcon(R.drawable.ic_action_save_b)
                            .setPositiveButton(android.R.string.yes,
                                    new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    Snackbar.make(view, "Saving NBT data...", Snackbar.LENGTH_LONG)
                                            .setAction("Action", null).show();
                                    if(nbt.save()){
                                        //nbt is not "modified" anymore, in respect to the new saved data
                                        nbt.modified = false;

                                        Snackbar.make(view, "Saved NBT data!", Snackbar.LENGTH_LONG)
                                                .setAction("Action", null).show();
                                        ((WorldActivityInterface) activity).logFirebaseEvent(WorldActivity.CustomFirebaseEvent.NBT_EDITOR_SAVE);
                                    } else {
                                        Snackbar.make(view, "Error: failed to save the NBT data.", Snackbar.LENGTH_LONG)
                                                .setAction("Action", null).show();
                                    }
                                }})
                            .setNegativeButton(android.R.string.no, null).show();
                }

            }
        });

        return rootView;
    }


    @Override
    public void onStart() {
        super.onStart();

        getActivity().setTitle("NBT editor");

        Bundle bundle = new Bundle();
        bundle.putString("title", nbt.getRootTitle());

        ((WorldActivityInterface) getActivity()).logFirebaseEvent(WorldActivity.CustomFirebaseEvent.NBT_EDITOR_OPEN, bundle);
    }

    @Override
    public void onResume() {
        super.onResume();

        ((WorldActivityInterface) getActivity()).showActionBar();

    }
}