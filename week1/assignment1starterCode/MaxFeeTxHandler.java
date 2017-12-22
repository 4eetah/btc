import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.Arrays;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class TxHandler {
    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS
        // (1)

        for (Transaction.Input in : tx.getInputs()) {
            UTXO in_utxo = new UTXO(in.prevTxHash, in.outputIndex);
            if (!utxoPool.contains(in_utxo)) {
                return false;
            }
        }

        // (2)
        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input currTxIn = tx.getInput(i);
            UTXO in_utxo = new UTXO(currTxIn.prevTxHash, currTxIn.outputIndex);
            Transaction.Output prevTxOut = utxoPool.getTxOutput(in_utxo);
            if (!Crypto.verifySignature(prevTxOut.address, tx.getRawDataToSign(i), currTxIn.signature)) {
                return false;
            }
        }
         
        double outSum, inSum;
        inSum = outSum = 0.0;

        // (3)
        Set<UTXO> utxoClaimedSet = new HashSet<UTXO>();
        for (Transaction.Input in : tx.getInputs()) {
            UTXO in_utxo = new UTXO(in.prevTxHash, in.outputIndex);
            if (utxoClaimedSet.contains(in_utxo)) {
                return false;
            }
            utxoClaimedSet.add(in_utxo);
            Transaction.Output out = utxoPool.getTxOutput(in_utxo);
            inSum += out.value;
        }

        // (4)
        for (Transaction.Output out : tx.getOutputs()) {
            if (out.value < 0)
                return false;
            outSum += out.value;
        }

        // (5)
        if (outSum > inSum)
            return false;

        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        ArrayList<Transaction> T = new ArrayList<Transaction>();
        for (Transaction tx : possibleTxs) {
            if (isValidTx(tx)) {
                T.add(tx);
                for (Transaction.Input in : tx.getInputs()) {
                    UTXO old_utxo = new UTXO(in.prevTxHash, in.outputIndex);
                    utxoPool.removeUTXO(old_utxo);
                }
                for (int i = 0; i < tx.numOutputs(); i++) {
                    UTXO new_utxo = new UTXO(tx.getHash(), i);
                    utxoPool.addUTXO(new_utxo, tx.getOutput(i));
                }
            }
        }

        Transaction[] retTxs = new Transaction[T.size()];
        int i=0;
        for (Transaction tx : T) {
            retTxs[i++] = tx;
        }

        return retTxs;
    }


    public static void main(String[] args) {
    }
}
