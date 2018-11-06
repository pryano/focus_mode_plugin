import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

class SelfCleaningEditorFactoryListener implements EditorFactoryListener {
    private HashMap<Editor, FocusModeCaretListener> editorToFocusModeListenerMap = new HashMap<>();

    // Add FocusModeListener to all new editors
    @Override
    public void editorCreated(@NotNull EditorFactoryEvent event) {
        final Editor editor = event.getEditor();
        FocusModeCaretListener fl = new FocusModeCaretListener(editor);
        editor.getCaretModel().addCaretListener(fl);
        this.editorToFocusModeListenerMap.put(editor, fl);
    }

    // Remove FocusModeListener from closing editors
    @Override
    public void editorReleased(@NotNull EditorFactoryEvent event) {
        final Editor editor = event.getEditor();
        FocusModeCaretListener fl = this.editorToFocusModeListenerMap.remove(editor);
        editor.getCaretModel().removeCaretListener(fl);
        fl.cleanup();
    }

    void cleanUp(){
        for(Map.Entry<Editor, FocusModeCaretListener> entry: this.editorToFocusModeListenerMap.entrySet()){
            Editor editor = entry.getKey();
            FocusModeCaretListener listener = entry.getValue();
            editor.getCaretModel().removeCaretListener(listener);
            listener.cleanup();
        }
        this.editorToFocusModeListenerMap.clear();
        this.editorToFocusModeListenerMap = null;
    }

    void addEditorAndListener(Editor editor, FocusModeCaretListener listener){
        // TODO: only put if there isn't an editor already
        this.editorToFocusModeListenerMap.put(editor, listener);
    }
}
