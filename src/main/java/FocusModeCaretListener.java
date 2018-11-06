import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
// import com.jetbrains.python.psi.PyClass;
// import com.jetbrains.python.psi.PyFunction;

import java.awt.*;
import java.util.ArrayList;


class FocusModeCaretListener implements CaretListener {
    private final ArrayList<RangeHighlighter> rangeHighlighters = new ArrayList<>();
    private final Editor editor;

    public FocusModeCaretListener(Editor editor){
        this.editor = editor;
    }

    public void cleanup(){
        this.removeRanges(this.editor);
        this.rangeHighlighters.clear();
    }

    @Override
    public void caretRemoved(CaretEvent event) {
        this.cleanup();
    }

    @Override
    public void caretPositionChanged(final CaretEvent event) {
        final Editor editor = event.getEditor();
        final Document document = editor.getDocument();
        final Project project = editor.getProject();

        // clear the previous highlights
        this.removeRanges(editor);

        // The following is needed to sync the psiFile. See https://bit.ly/2zdonSn
        assert project != null;
        PsiDocumentManager.getInstance(project).commitDocument(document);

        // find method/class that the cursor resides in
        // Class[] blockTypes = new Class[]{PsiMethod.class, PsiClass.class};
        Class[] blockTypes = new Class[]{PsiClass.class};
        TextRange blockRange = this.getCodeBlockRange(editor, project, document, blockTypes);
        if (blockRange == null) {
            return;
        }

        // Get the range before and after the code block
        TextRange[] invertedRange = this.invertTextRange(document, blockRange);
        TextRange preRange = invertedRange[0];
        TextRange postRange = invertedRange[1];

        // add highlights
        if (preRange != null && preRange.getEndOffset() > 0) {
            RangeHighlighter preHighlight = this.addRange(editor, preRange);
            this.rangeHighlighters.add(preHighlight);
        }
        if (postRange != null && postRange.getEndOffset() > postRange.getStartOffset() + 1) {
            RangeHighlighter postHighlight = this.addRange(editor, postRange);
            this.rangeHighlighters.add(postHighlight);
        }
    }

    private TextRange getCodeBlockRange(final Editor editor, final Project project, final Document document, Class[] allowedTypes){
        final PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(project);
        if (psiDocumentManager == null || document == null){
            return null;
        }
        final PsiFile psiFile = psiDocumentManager.getPsiFile(document);
        if (psiFile == null){
            return null;
        }

        int offset = editor.getCaretModel().getOffset();
        PsiElement element = psiFile.findElementAt(offset);
        for(Class<PsiElement> elementClass: allowedTypes){
            PsiElement enclosingElement = PsiTreeUtil.getParentOfType(element, elementClass);
            if (enclosingElement != null){
                return enclosingElement.getTextRange();
            }
        }

        return null;
    }

    private TextRange[] invertTextRange(Document document, TextRange textRange) {
        int methodStartOffset = textRange.getStartOffset();
        int methodLineNumber = document.getLineNumber(methodStartOffset);
        int previousLineOffset = document.getLineEndOffset(methodLineNumber - 1);
        TextRange preRange = new TextRange(0, previousLineOffset);

        int methodEndOffset = textRange.getEndOffset();
        int methodEndLineNumber = document.getLineNumber(methodEndOffset);
        int nextLineOffset = document.getLineStartOffset(methodEndLineNumber+1);
        TextRange postRange = new TextRange(nextLineOffset, document.getTextLength());

        return new TextRange[]{preRange, postRange};
    }

    private RangeHighlighter addRange(final Editor editor, TextRange range){
        // The new text color should be a slightly more visible shade of the background
        Color background = editor.getColorsScheme().getDefaultBackground();
        int totalBackgroundColor = background.getRed() + background.getBlue() + background.getGreen();
        Color newTextColor = (totalBackgroundColor > 380) ? background.darker() : background.brighter();

        TextAttributes style = new TextAttributes(newTextColor, null, null, null, 0);
        int layer = HighlighterLayer.LAST - 1; // = highest layer

        MarkupModel markupModel = editor.getMarkupModel();

        return markupModel.addRangeHighlighter(
                range.getStartOffset(),
                range.getEndOffset(),
                layer,
                style,
                HighlighterTargetArea.LINES_IN_RANGE
        );
    }

    private void removeRanges(Editor editor) {
        if (this.rangeHighlighters.isEmpty()){
            return;
        }
        final MarkupModel markupModel = editor.getMarkupModel();
        for(RangeHighlighter rh: this.rangeHighlighters) {
            try {
                markupModel.removeHighlighter(rh);
            } catch (AssertionError ignored) {}
        }
        this.rangeHighlighters.clear();
    }
}
