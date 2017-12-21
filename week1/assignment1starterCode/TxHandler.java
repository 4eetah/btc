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

    private Set<Transaction.Output> getAllUTXOTxOutputs() {
        Set<Transaction.Output> utxoTxOutputSet = new HashSet<Transaction.Output>();
        for (UTXO u : utxoPool.getAllUTXO()) {
            Transaction.Output out = utxoPool.getTxOutput(u);
            utxoTxOutputSet.add(out);
        }
        return utxoTxOutputSet;
    }
    private void printTxOutputs(Iterable<Transaction.Output> txout) {
        System.out.println("Tx.Outpus for " + txout);
        for (Transaction.Output out : txout)
            System.out.println("Tx.Output: value: " + out.value + ", address: " + out.address);
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
        ArrayList<Transaction> res = new ArrayList<Transaction>();

        for (int i=0; i < possibleTxs.length; i++) {
            for (int nrout=0; nrout < possibleTxs[i].numOutputs(); nrout++) {
                UTXO currUTXO = new UTXO(possibleTxs[i].getHash(), nrout);
                if(!utxoPool.contains(currUTXO)) {
                    utxoPool.addUTXO(currUTXO, possibleTxs[i].getOutput(nrout));
                }
            }
            if (isValidTx(possibleTxs[i]))
                res.add(possibleTxs[i]);
        }
        Transaction[] retTxs = new Transaction[res.size()];
        int i=0;
        for (Transaction tx : res) {
            retTxs[i++] = tx;
        }

        return retTxs;
    }


    public static void main(String[] args) {
        int n = 3;
        Transaction[] tx = new Transaction[n];
        for (int i = 0; i < n; i++)
            tx[i] = new Transaction();

        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException x) {
            System.out.println(x.getMessage());
            return;
        }

        byte[][] prevHashes = new byte[3][];
        String[] prevDummy = new String[n];
        for (int i=0; i<n; i++) {
            prevDummy[i] = new String("Hello"+i);
        }
        for(int i = 0; i < n; i++) {
            byte b[] = prevDummy[i].getBytes();
            md.update(b);
            prevHashes[i]=md.digest();
        }
        tx[0].addInput(prevHashes[0], 0);
        tx[1].addInput(prevHashes[0], 0);
        tx[1].addInput(prevHashes[1], 1);
        tx[2].addInput(prevHashes[0], 0);
        tx[2].addInput(prevHashes[1], 1);
        tx[2].addInput(prevHashes[2], 2);
    }
}
