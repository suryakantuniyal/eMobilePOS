package interfaces;

import com.android.support.CreditCardInfo;

public interface EMSCallBack {
    void cardWasReadSuccessfully(boolean read, CreditCardInfo cardManager);

    void readerConnectedSuccessfully(boolean value);

    void scannerWasRead(String data);

    void startSignature();

    void nfcWasRead(String nfcUID);
}

