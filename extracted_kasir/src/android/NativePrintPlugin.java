package id.yogafernando.kasiridris;

import android.content.Context;
import android.print.PrintManager;
import android.webkit.WebView;
import android.print.PrintAttributes;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "NativePrint")
public class NativePrintPlugin extends Plugin {
    @PluginMethod
    public void print(PluginCall call) {
        getActivity().runOnUiThread(() -> {
            try {
                WebView webView = getBridge().getWebView();
                PrintManager printManager =
                    (PrintManager) getContext().getSystemService(Context.PRINT_SERVICE);
                if (printManager == null || webView == null) {
                    call.reject("Layanan cetak Android tidak tersedia.");
                    return;
                }
                PrintAttributes attributes = new PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.NA_LETTER)
                    .setResolution(new android.print.PrintAttributes.Resolution(
                        "kasir_idris", "Kasir Idris", 300, 300))
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build();
                printManager.print(
                    "Struk-Kasir-Idris",
                    webView.createPrintDocumentAdapter("Struk Kasir Idris"),
                    attributes
                );
                call.resolve();
            } catch (Exception e) {
                call.reject("Gagal membuka cetak Android: " + e.getMessage(), e);
            }
        });
    }
}
