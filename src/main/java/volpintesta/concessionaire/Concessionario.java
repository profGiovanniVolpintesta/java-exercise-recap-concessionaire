/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package volpintesta.concessionaire;

/**
 *
 * @author User
 */
public class Concessionario
{
    /**
     * Crea un concessionario con una lista vuota di veicoli e un saldo iniziale
     * di 0 euro.
     */
    public Concessionario()
    {
        
    }

    /**
     * Aggiunge un veicolo al concessionario verificando che non esistano veicoli
     * con la stessa targa. Nel caso esista già un veicolo con la stessa targa,
     * il veicolo non verrà aggiunto e verrà restituito false.
     * @param v il veicolo da aggiungere
     * @return true se il veicolo è stato aggiunto, false altrimenti
     */
    public boolean aggiungiVeicolo(Veicolo v)
    {
        return false;
    }
    
    /**
     * Mostra in output tutti i veicoli del catalogo
     */
    public void stampaCatalogo()
    {
        
    }
    
    /**
     * Vende il veicolo con la targa specificata e incrementa il saldo corrente
     * del concessionario della cifra ricavata dalla vendita (il prezzo del veicolo).
     * Se la vendita riesce restituisce true. Se la vendita non riesce, perchè non viene
     * trovato alcun veicolo con la targa specificata, restituisce false.
     * @param targa la targa del veicolo da vendere
     * @return true se la vendita viene effettuata, false se il veicolo non viene trovato
     */
    public boolean vendiVeicolo (String targa)
    {
        return false;
    }
    
    /**
     * Restituisce un veicolo con la targa specificata. Restituisce null se
     * non trova nessun veicolo con la targa specificata.
     * @param targa la targa da cercare
     * @return il veicolo con la targa specificata
     */
    public Veicolo cercaVeicolo (String targa)
    {
        return null;
    }
    
    /**
     * Restituisce l'array contenente i veicoli con la marca specificata.
     * L'array restituito non avrà elementi a null, quindi sarà esattamente della
     * dimensione richiesta.
     * @param marca la marca da cercare
     * @return un array contenente i veicoli con la marca specificata
     */
    public Veicolo[] cercaPerMarca (String marca)
    {
        return new Veicolo[]{};
    }
    
    /**
     * Restituisce l'array contenente i veicoli prodotti in un anno compreso tra gli
     * estremi specificati.
     * L'array restituito non avrà elementi a null, quindi sarà esattamente della
     * dimensione richiesta.
     * @param annoMin l'estremo inferiore dell'intervallo della ricerca (inclusivo)
     * @param annoMax l'estremo superiore dell'intervallo della ricerca (inclusivo)
     * @return un array contenente i veicoli prodotti in un anno compreso tra gli estremi specificati
     */
    public Veicolo[] cercaPerAnno (int annoMin, int annoMax)
    {
        return new Veicolo[]{};
    }
}
