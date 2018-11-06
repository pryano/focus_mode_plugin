import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;

public class FocusModeToggleAction extends AnAction {
    private static final String MENU_TEXT_ENTER = "Enter Focus Mode";
    private static final String MENU_TEXT_EXIT = "Exit Focus Mode";

    private boolean isFocusModeEnabled;
    private EditorFactory editorFactory = null;
    private SelfCleaningEditorFactoryListener editorFactoryListener = null;

    public FocusModeToggleAction() {
        this.isFocusModeEnabled = false;
    }

    public void actionPerformed(AnActionEvent event) {
        this.isFocusModeEnabled = !this.isFocusModeEnabled;
        String menuText = this.isFocusModeEnabled ? MENU_TEXT_EXIT : MENU_TEXT_ENTER;
        event.getPresentation().setText(menuText);

        if (this.isFocusModeEnabled) {
            this.addEditorFactoryListeners();
            this.addOpenEditorListeners(this.editorFactoryListener);
        } else {
            this.removeEditorFactoryListeners();
        }
    }

    private void addEditorFactoryListeners(){
        if (this.editorFactory == null) {
            this.editorFactory = EditorFactory.getInstance();
        }

        SelfCleaningEditorFactoryListener efl = new SelfCleaningEditorFactoryListener();
        this.editorFactoryListener = efl;
        this.editorFactory.addEditorFactoryListener(efl);
    }

    private void addOpenEditorListeners(SelfCleaningEditorFactoryListener listener){
        // get all the active editors and add the listeners here
        //   on editor close, the listeners should close
        Editor[] allEditors = this.editorFactory.getAllEditors();
        for(Editor e: allEditors){
            FocusModeCaretListener fl = new FocusModeCaretListener(e);
            e.getCaretModel().addCaretListener(fl);
            listener.addEditorAndListener(e, fl);
        }
    }

    private void removeEditorFactoryListeners(){
        this.editorFactory.removeEditorFactoryListener(this.editorFactoryListener);
        this.editorFactoryListener.cleanUp();
        this.editorFactoryListener = null;
        this.editorFactory = null;
    }
}
