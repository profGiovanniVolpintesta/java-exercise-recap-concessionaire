/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package volpintesta.concessionaire;

import java.util.Scanner;

/**
 *
 * @author User
 */
public class MainClass
{
    private static Scanner input = new Scanner(System.in);
    
    /**
     * Il seguente main funziona soltanto fino alla parte 3
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        Concessionario c = new Concessionario(); // crea un concessionario vuoto
        c.aggiungiVeicolo(new Veicolo("AA 111 AA", "BMW", "X1", 2020, 40000));
        c.aggiungiVeicolo(new Veicolo("BB 222 BB", "BMW", "X1", 2022, 50000));
        c.aggiungiVeicolo(new Veicolo("CC 333 CC", "BMW", "X3", 2023, 70000));
        c.aggiungiVeicolo(new Veicolo("DD 444 DD", "Ferrari", "F40", 1985, 3000000));
        c.aggiungiVeicolo(new Veicolo("EE 555 EE", "Lamborghini Urus", 2021, 245000));
        c.stampaCatalogo();     
        
        System.out.println("----------------------");
        mostraMenu();
        
        boolean continua = true;
        do
        {
            String targa = "";
            String marca = "";
            String modello = "";
            int anno = 0;
            int annoMin = 0;
            int annoMax = 0;
            float prezzo = 0;
            Veicolo veicolo = null;
            boolean successo = false;
            Veicolo[] veicoli = new Veicolo[]{};

            System.out.println("----------------------");
            System.out.print("Cosa vuoi fare? ");
                       
            switch (input.nextLine())
            {
                case "1":                    
                    System.out.println("Inserimento di un nuovo veicolo");
                    System.out.print(" >>> Targa: ");
                    targa = input.nextLine();
                    System.out.print(" >>> Marca: ");
                    marca = input.nextLine();
                    System.out.print(" >>> Modello: ");
                    modello = input.nextLine();                    
                    System.out.print(" >>> Anno di produzione: ");
                    anno = Integer.parseInt(input.nextLine());                                        
                    System.out.print(" >>> Prezzo (€): ");
                    prezzo = Float.parseFloat(input.nextLine());
                    
                    veicolo = new Veicolo(targa, marca, modello, anno, prezzo);                    
                    successo = c.aggiungiVeicolo(veicolo);
                    if (successo)
                        System.out.println("Veicolo aggiunto con successo!");
                    else
                        System.out.println("ERRORE: non può essere aggiunto un veicolo con la stessa targa di uno già esistente.");
                    
                    break;
                    
                case "2":
                    System.out.println("Vendita di un veicolo");
                    System.out.print(" >>> Targa: ");
                    targa = input.nextLine();
                    successo = c.vendiVeicolo(targa);
                    if (successo)
                        System.out.println("Veicolo venduto con successo!");
                    else
                        System.out.println("Non è stato trovato nessun veicolo con la targa richiesta.");
                    break;
                    
                case "3":
                    System.out.println("Ricerca per marca");
                    System.out.print(" >>> Marca: ");
                    marca = input.nextLine();
                    veicoli = c.cercaPerMarca(marca);
                    for (Veicolo v : veicoli)
                        if (v != null)
                            System.out.println(v);
                    break;
                    
                case "4":
                    System.out.println("Ricerca per anno tra due date");
                    System.out.print(" >>> dall'anno: ");
                    annoMin = Integer.parseInt(input.nextLine());
                    System.out.print(" >>> all'anno: ");
                    annoMax = Integer.parseInt(input.nextLine());
                    veicoli = c.cercaPerMarca(marca);
                    for (Veicolo v : veicoli)
                        if (v != null)
                            System.out.println(v);
                    break;
                    
                case "5":
                    System.out.println("Ricerca per targa");
                    System.out.print(" >>> Targa: ");
                    targa = input.nextLine();
                    veicolo = c.cercaVeicolo(targa);
                    if (veicolo != null)
                        System.out.println(veicolo);
                    else
                        System.out.println("Veicolo non trovato!");
                    break;
                    
                case "0":
                    continua = false;
                    break;
                    
                case "?":
                    mostraMenu();
                    break;
                    
                default:
                    System.out.println("Scelta non valida.");
            }
        } while (continua);
    }
    
    public static void mostraMenu()
    {
        System.out.println("--- MENU DEI COMANDI ---");
        System.out.println("1 - Aggiungi auto (veicolo)");
        System.out.println("2 - Aggiungi moto (veicolo)");
        System.out.println("3 - Vendi veicolo");
        System.out.println("4 - Cerca per marca");
        System.out.println("5 - Cerca per anno");
        System.out.println("6 - Cerca per targa");
        System.out.println("0 - esci");
        System.out.println("? - Menu dei comandi");
    }
}
