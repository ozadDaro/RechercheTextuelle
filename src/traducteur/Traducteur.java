package traducteur;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import tableau_suffixe.MonolingualCorpus;
import tableau_suffixe.SuffixArray;

public class Traducteur {
	private SuffixArray suffixArray_lang1;	// fr
	private SuffixArray suffixArray_lang2;	// eng
	//private HashMap<Integer, Integer> link;
	private ListCoupleInt link;
	
	public Traducteur(String lang1, String lang2, String corpus, String link){
		suffixArray_lang1 = new SuffixArray(corpus, lang1);		
		suffixArray_lang2 = new SuffixArray(corpus, lang2);
		
		String fileName_link = "";
		initLink(fileName_link, link);
	}

	/**
	 * Initialise la hashmap qui contient les liens entre les phrases des 2 corpus
	 * @param read_write_link
	 * @param load_link
	 */
	private void initLink(String read_write_link, String load_link) {
		//link = new HashMap<Integer, Integer>();
		link = new ListCoupleInt();
		// Lecture
		if(isWritenInFile(read_write_link)){
			if(!readLink(read_write_link)){
				System.err.println("Erreur dans la lecture des liens dans "
						+ "le fichier -" + read_write_link + "-");
				System.exit(0);
			}
		}
		// Ecriture
		else{
			if(!loadLink(load_link)){
				System.err.println("Erreur dans le chargement du fichier -" + load_link + "-");
				System.exit(0);
			}
			if(!writeLink(read_write_link)){
				System.err.println("Erreur dans l'ecriture des liens dans "
						+ "le fichier -" + read_write_link + "-");
				System.exit(0);
			}
		}
	}
	
	/**
	 * Ecrit dans un fichier les phrases des 2 corpus en parallele
	 * @param fileLang1
	 * @param fileLang2
	 * @return
	 */
	/**
	 * TODO : verifier s'il faut faire newLine + flush
	 */
	public boolean writePhrasesInParallel(String fileLang1, String fileLang2){
		try {
			BufferedWriter bw_lang1 = new BufferedWriter(new FileWriter(fileLang1));
			BufferedWriter bw_lang2 = new BufferedWriter(new FileWriter(fileLang2));
			
			int id_ligne;
			String phrase;
			
			boolean res = true;
			for(CoupleInt c : link){
				// On s'occupe de la phrase du 1er int
				id_ligne = c.getI1();
				phrase = suffixArray_lang1.getCorpus().getPhraseFromLine(id_ligne);
				// cas ou la ligne est presente dans le corpus lang1
				if(phrase != null){
					bw_lang1.write(phrase);
					bw_lang1.newLine();
					bw_lang1.flush();
					
					// on s'occupe de la phrase du 2eme int : elle devrait
					// appartenir au corpus lang2
					id_ligne = c.getI2();
					phrase = suffixArray_lang2.getCorpus().getPhraseFromLine(id_ligne);
					if(phrase != null){
						bw_lang2.write(phrase);
						bw_lang2.newLine();
						bw_lang2.flush();
					}
					else{
						System.err.println("Erreur au niveau du CoupleInt " + c + " car la phrase "
								+ c.getI2() + " n'appartient pas au corpus "
										+ suffixArray_lang2.getCorpus().getLangue());
						res = false;
					}
				}
				else{
					phrase = suffixArray_lang2.getCorpus().getPhraseFromLine(id_ligne);
					// cas ou la ligne est presente dans le corpus lang2
					if(phrase != null){
						bw_lang2.write(phrase);
						bw_lang2.newLine();
						bw_lang2.flush();
						
						// on s'occupe de la phrase du 2eme int : elle devrait
						// appartenir au corpus lang1
						id_ligne = c.getI2();
						phrase = suffixArray_lang1.getCorpus().getPhraseFromLine(id_ligne);
						if(phrase != null){
							bw_lang1.write(phrase);
							bw_lang1.newLine();
							bw_lang1.flush();
						}
						else{
							System.err.println("Erreur au niveau du CoupleInt " + c + " car la phrase "
									+ c.getI2() + " n'appartient pas au corpus "
											+ suffixArray_lang1.getCorpus().getLangue());
							res = false;
						}
					}
					// Erreur : la ligne n'appartient a aucun des corpus
					else{
						System.err.println("Erreur au niveau du CoupleInt " + c + " car la phrase "
								+ c.getI1() + " n'appartient a aucun corpus");
						res = false;
					}
				}
				
			}
		  
		 	bw_lang1.close();
		 	bw_lang2.close();
		 	return res;
		} 
		catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
		
	/**
	 * Ecrit la hashmap dans un fichier
	 * @param fileName
	 * @return
	 */
	private boolean writeLink(String fileName) {
		try {
			// Ouverture flux
			File f = new File(fileName);
			f.createNewFile();
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f));

			// serialization de l'objet
			oos.writeObject(link);

			// Fermeture flux
			oos.close();
			return true;

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}		
	}

	/**
	 * Chargement des liens dans le fichier fourni par tatoeba
	 * @param fileName
	 * @return
	 */
	private boolean loadLink(String fileName) {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(
					new FileInputStream(fileName), "UTF8"));
			String ligne;
			String[] tab;
			// On parcours l'ensemble du fichier link
			while ((ligne = br.readLine()) != null) {
				// On parse la ligne en enlevant les espaces
				tab = ligne.split(" ");
				
				// Condition pour ajouter le couple d'entier : il faut que les phrases liees
				// appartiennent aux 2 corpus
				if((suffixArray_lang1.getCorpus().getTab_token().containsKey(tab[0])
						&& suffixArray_lang2.getCorpus().getTab_token().containsKey(tab[1]))
						||
				   (suffixArray_lang2.getCorpus().getTab_token().containsKey(tab[0]) 
						&& suffixArray_lang1.getCorpus().getTab_token().containsKey(tab[1]))){
					link.add(new CoupleInt(Integer.parseInt(tab[0]), Integer.parseInt(tab[0])));
				}
			}
			return true;
		} catch (Exception e) {
			System.out.println(e.toString());
			return false;
		}
		
	}

	/**
	 * Charge la hashmap ecrite dans un fichier
	 * @param fileName
	 * @return
	 */
	private boolean readLink(String fileName) {
		try {
			// Ouverture du flux
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(fileName)));
			link = (ListCoupleInt) ois.readObject();
			// Fermeture du flux
			ois.close();

			return true;
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
			return false;
		}		
	}

	/**
	 * permet de savoir si il y a le fichier qui contient les structures
	 * 
	 * @param fileName
	 * @return true si un tel fichier existe, false sinon
	 */
	public boolean isWritenInFile(String fileName) {
		File f = new File(fileName);
		return f.exists();
	}
	
	/** Traduit tokens de la lang1 vers la lang2
	 * Phrase peut etre qu'un seul mot ou un groupe de mot
	 * @param phrase
	 * @return
	 */
	public ArrayList<String> traduct(String phrase, String lang1, String lang2){
		ArrayList<String> resultat_traduction = new ArrayList<String>();
		
		// Contient les id des phrases de la recherche dans la langue initiale
		ArrayList<Integer> list_IdPhrases;
		
		// Contient les id des phrases de la recherche dans la langue traduite
		ArrayList<Integer> list_IdPhrases_traduit = new ArrayList<Integer>();
		
		// Cas ou on veut traduire de suffixArray_lang1 -> suffixArray_lang2
		if(suffixArray_lang1.getCorpus().getLangue().equals(lang1) && 
				suffixArray_lang2.getCorpus().getLangue().equals(lang2)){
			list_IdPhrases = suffixArray_lang1.getAllPositionsOfPhrase(phrase);

			int pos_traduit;
			// On effectue la traduction
			for(Integer pos : list_IdPhrases){
				pos_traduit = link.getOtherInt(pos);
				if(pos_traduit != -1){
					list_IdPhrases_traduit.add(pos_traduit);	
				}
			}
			
			/**
			 * TODO : on doit chercher dans suffixArray_lang2 les 
			 * phrases dont les id sont dans list_IdPhrases_traduit
			 */
			int position_corpus;
			String phraseAtPosi;
			for(Integer pos : list_IdPhrases)
			{
				 //position_corpus = suffixArray_lang2.getCorpus().getTab_line().get_i1(pos);
				 //phraseAtPosi = suffixArray_lang2.getCorpus().getSuffixFromPosition(position_corpus);
				 //resultat_traduction.add(phraseAtPosi);
			}			
		}
		
		// Cas ou on veut traduire de suffixArray_lang2 -> suffixArray_lang1
		else if(suffixArray_lang2.getCorpus().getLangue().equals(lang1) && 
				suffixArray_lang1.getCorpus().getLangue().equals(lang2)){
			list_IdPhrases = suffixArray_lang2.getAllPositionsOfPhrase(phrase);
			
			int pos_traduit;
			// On effectue la traduction
			for(Integer pos : list_IdPhrases){
				pos_traduit = link.getOtherInt(pos);
				if(pos_traduit != -1){
					list_IdPhrases_traduit.add(pos_traduit);	
				}
			}
			
			/**
			 * TODO : on doit chercher dans suffixArray_lang1 les 
			 * phrases dont les id sont dans list_IdPhrases_traduit
			 */
			int position_corpus;
			String phraseAtPosi;
			for(Integer pos : list_IdPhrases)
			{
				// position_corpus = suffixArray_lang1.getCorpus().getTab_line().get_i1(pos);
			//	 phraseAtPosi = suffixArray_lang1.getCorpus().getSuffixFromPosition(position_corpus);
				// resultat_traduction.add(phraseAtPosi);
			}			
			
		}
		// Probleme
		else{
			System.err.println("Erreur : les langues sont differentes des langues des corpus");
		}
		return null;
	}
	
	
	/**
	 * Getter & Setter
	 */
	

	public SuffixArray getSuffixArray_lang1() {
		return suffixArray_lang1;
	}

	public void setSuffixArray_lang1(SuffixArray suffixArray_lang1) {
		this.suffixArray_lang1 = suffixArray_lang1;
	}

	public SuffixArray getSuffixArray_lang2() {
		return suffixArray_lang2;
	}

	public void setSuffixArray_lang2(SuffixArray suffixArray_lang2) {
		this.suffixArray_lang2 = suffixArray_lang2;
	}

	public ListCoupleInt getLink() {
		return link;
	}

	public void setLink(ListCoupleInt link) {
		this.link = link;
	}
}
