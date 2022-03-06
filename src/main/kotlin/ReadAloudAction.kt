import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import java.io.File
import java.util.concurrent.CompletableFuture

class ReadAloudAction : AnAction() {
    private var speaking: Boolean = false;
    private var textToSpeak : String? = "";
    private var path : File? = null;
    private var process : Process? = null;
    private var processWait : CompletableFuture<Process>? = null;


    override fun actionPerformed(e: AnActionEvent) {
        startSpeech();
    }

    override fun update(e: AnActionEvent) {
        checkSpeech();

        val project = e.project;
        val editor : Editor? = e.getData(CommonDataKeys.EDITOR);

        val hasEditor = project != null && editor != null;

        e.presentation.isEnabledAndVisible = hasEditor;
        if (!hasEditor) {
            return;
        }

        val selectionModel = editor?.selectionModel;
        val hasSelection = selectionModel?.hasSelection();

        if (hasSelection == false) {
            textToSpeak = "";
        } else {
            textToSpeak = selectionModel?.selectedText;
        }

        e.presentation.isEnabled = textToSpeak != null && textToSpeak!!.isNotEmpty();
    }

    fun checkSpeech() {
        if (this.speaking && this.processWait != null) {
            if (this.processWait!!.isDone) {
                this.speaking = false;
                this.processWait = null;
                this.process = null;

                this.path?.delete();
                this.path = null;
            }
        }
    }

    private fun startSpeech() {
        // don't start speaking if we're already speaking or there's nothing to speak.
        if (this.speaking) {
            return;
        } else if (this.textToSpeak?.trim()?.isEmpty() == true) {
            return
        }

        // allocate a new temporary file to store the text in.
        this.path = File.createTempFile("_espeak-text", ".tmp");

        // write the text to speak out to the file
        this.textToSpeak?.let { path?.writeText(it) };

        // instruct the espeak-ng command to start speaking the contents of the file.
        this.process = ProcessBuilder("espeak-ng", "--stdin")
            .redirectInput(path)
            .start();

        this.processWait = this.process!!.onExit();

        this.speaking = true;
    }
}