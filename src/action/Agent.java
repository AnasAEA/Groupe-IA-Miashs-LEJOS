package agent;

import java.util.ArrayList;

import action.Deplacement;
import lejos.hardware.motor.NXTRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;
import lejos.robotics.navigation.MovePilot;
import perception.CapteurTouche;
import perception.CapteurUltrason;
import perception.ColorSensor;

public class Agent {
    // Existing class attributes
    private CapteurUltrason capteurUltrason;
    private CapteurTouche capteurTouche;
    private Deplacement deplacement;
    private ArrayList<float[]> liste = new ArrayList<>();
    private float directionCampAdverse; // Déclaration en tant qu'attribut de classe
    private NXTRegulatedMotor motor; 
    private Pince pince ;
    private ColorSensor couleur;

    // Constructor
    public Agent() {
        capteurUltrason = new CapteurUltrason(SensorPort.S1);
        capteurTouche = new CapteurTouche(SensorPort.S2);
        deplacement = new Deplacement();
	motor = new NXTRegulatedMotor (MotorPort.C);
        pince = new Pince( motor);
        couleur = new ColorSensor(SensorPort.S2);
        
    }

    /**
     * Détecte tout objet a une distance inférieure é 50 cm.
     * 
     * @return true si un objet est détecté, false sinon.
     */

    // Method to detect objects by rotating 360 degrees
    public ArrayList<float[]> detecterLesObjets() {
        deplacement.getPilot().setAngularSpeed(30); // Régler la vitesse de rotation
        deplacement.tournerAsync(360); // Démarrer une rotation de 360 degrés
        MovePilot pilot = deplacement.getPilot();
        this.liste.clear();

        while (pilot.isMoving()) {
            float distance = capteurUltrason.getDistance();
            float directionActuelle = deplacement.getDirection(); // Obtenir l'orientation actuelle

            // Collecter les données si la distance est inférieure à un certain seuil
            if (distance < 60.0f) {
                float[] objet = { distance, directionActuelle };
                this.liste.add(objet);
                System.out.println("Objet détecté é une distance de : " + distance + " cm, direction : "
                        + directionActuelle + " degrés.");
            }

            // Pause pour éviter de surcharger le processeur
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        pilot.stop(); // S'assurer que le robot arréte de tourner
        return this.liste;
    }
public void attraperPalet() {
    if (!pince.isEtat()) { 
        System.out.println("La pince est ouverte. Fermeture pour attraper le palet.");
        pince.fermerPince(); 
        pince.setaPalet(true);
    } else {
        System.out.println("La pince est déjà fermée. Impossible d'attraper un autre palet.");
    }
}

public void lacherPalet() {
    if (pince.isEtat()) { 
        System.out.println("Relâchement du palet.");
        pince.ouvrirPince(); 
        pince.setaPalet(false); 
    } else {
        System.out.println("La pince est déjà ouverte. Aucun palet à relâcher.");
    }
}


    // Method to find the closest object
    public float[] bestObjet(ArrayList<float[]> objets) {
        if (objets.isEmpty()) {
            return null;
        }
        float minDistance = Float.MAX_VALUE;
        float[] bestObjet = null;
        for (float[] objet : objets) {
            float distance = objet[0];
            if (distance < minDistance) {
                minDistance = distance;
                bestObjet = objet;
            }
        }
        return bestObjet;
    }

    /**
     * Surveille la présence d'obstacles et effectue une esquive si un obstacle est
     * détecté.
     *
     * @return true si un obstacle était détecté et esquivé, false sinon.
     */
    public boolean Surveiller() {
        if (detectObjet()) {
            System.out.println("Obstacle détecté lors de la surveillance. Initiation de l'évitement...");
            esquive();
            return true;
        }
        return false;
    }

    private void marquerPalet() {
        // Ajuster l'orientation du robot vers le camp adverse
        float currentDirection = deplacement.getDirection();
        float angleToTurn = directionCampAdverse - currentDirection;

        // Ajuster l'angle pour qu'il se situe entre -180 et 180 degrés
        if (angleToTurn > 180) {
            angleToTurn -= 360;
        } else if (angleToTurn < -180) {
            angleToTurn += 360;
        }

        // Déplacer le robot vers la ligne blanche (camp adverse)
        versCouleur(White);

        // Avancer d'environ 5 cm pour s'assurer que le palet se trouve bien dans le
        // camp adverse
        deplacement.avancer(5);
        // Lâcher le palet
        lacherPalet();
        deplacement.avancer(-10);
    }
	 public void versCouleur(String color) {
    	while (!(couleur.getColorName() == color)) {
    		deplacement.avancerAsync(directionCampAdverse);
    	}
    	deplacement.stop();	
    }

    public boolean ChercherPalet() {
        ArrayList<float[]> objets = detecterLesObjets();

        // Find the best object (closest)
        float[] bestObj = bestObjet(objets);

        if (bestObj != null) {
            float distanceToObject = bestObj[0];
            float directionToObject = bestObj[1];

            System.out.println("Best object found at distance: " + distanceToObject + " cm, direction: "
                    + directionToObject + " degrees.");
            deplacement.getPilot().setAngularSpeed(30); // Régler la vitesse de rotation
            // Calculate angle to turn
            float currentDirection = deplacement.getDirection();
            float angleToTurn = directionToObject - currentDirection;

            // Compensate for the 7-degree deviation
            if (angleToTurn > 0) {
                angleToTurn -= 7; // If turning clockwise, reduce angle
            } else if (angleToTurn < 0) {
                angleToTurn += 7; // If turning counter-clockwise, increase angle
            }

            // Adjust angle to be between -180 and 180 degrees
            if (angleToTurn > 180) {
                angleToTurn -= 360;
            } else if (angleToTurn < -180) {
                angleToTurn += 360;
            }

            // Rotate to face the object
            deplacement.getPilot().rotate(angleToTurn);

            // Check if it's a palet
            boolean isPalet = capteurUltrason.detecterPalet();
            return isPalet;
        }
        return false;
    }
    
    public void ligneCentrale() {
	   //apres avoir deposer le palet on tourne 180 deg
	   deplacement.tournerAsync(180); 
	   //on avance jusqu'à que la couleur detectée est Black (ligne centrale)
	   versCouleur("Black");
    }
	 
    public void premierPalet() {
    	while (!(capteurTouche.isPressed())) {
    	deplacement.avancerAsync(directionCampAdverse);
    	//esquive();
    	}
    	deplacement.stop();
    	pince.ouvrirPince();
    	pince.fermerPince();
    	deplacement.tournerAsync(90);
    	deplacement.avancerAsync(15);
    	deplacement.tournerAsync(90);
    	Agent a = new Agent();
    	a.versCouleur("White");
    	deplacement.avancerAsync(5);
    	pince.ouvrirPince();
    	System.out.print( "le palet est déposé");
    	pince.fermerPince();
    }
     public void secondPalet() {
    	 deplacement.tournerAsync(180);
    	 Agent a = new Agent();
     	 a.versCouleur("Bleu");
     	deplacement.tournerAsync(-90);
     	while (!(capteurUltrason.detecterPalet())){
     		deplacement.avancerAsync(100);
     	}
     	while (!(capteurTouche.isPressed())) {
        	deplacement.avancerAsync(directionCampAdverse);
        	//esquive();
        	}
        	deplacement.stop();
        	pince.ouvrirPince();
        	pince.fermerPince();
        	deplacement.tournerAsync(-90);
        	Agent b = new Agent();
        	b.versCouleur("White");
        	deplacement.avancerAsync(5);
        	pince.ouvrirPince();
        	System.out.print( "le palet est déposé");
        	pince.fermerPince();
        }
     public void troisiemePalet() {
    	 Agent c = new Agent();
    	 c.secondPalet();
     }
    public void run() {
        // Initialiser la direction du camp adverse
        directionCampAdverse = deplacement.getDirection();
        System.out
                .println("Direction du camp adverse initialement réglée à : " + directionCampAdverse + " degrés.");

        // Cleanup resources
        capteurUltrason.close();
        capteurTouche.close();
        deplacement.stop();
        System.out.println("Program terminated.");
    }
public void verscouleurs() {
    String couleurCourante = couleur.getColorName(); // Obtenir la couleur initiale
    System.out.println("Couleur initiale détectée : " + couleurCourante);

    while (!Button.ESCAPE.isDown()) { // La boucle se poursuit tant que l'utilisateur ne quitte pas
        deplacement.avancerAsync(10); // Avancer sur une petite distance en continu
        Delay.msDelay(100); // Pause pour éviter de lire trop rapidement les données du capteur

        String nouvelleCouleur = couleur.getColorName(); // Lire la nouvelle couleur
        if (!nouvelleCouleur.equals(couleurCourante)) { // Comparer avec la dernière couleur détectée
            deplacement.stop(); // Arrêter le mouvement
            System.out.println("Nouvelle couleur détectée : " + nouvelleCouleur);
            couleurCourante = nouvelleCouleur; // Mettre à jour la couleur courante

            // Pause pour permettre une observation ou une action avant de continuer
            Delay.msDelay(1000);
        }
    }
    System.out.println("Fin de la détection des couleurs.");
}
public void esquive() {
    System.out.println("Détection d'obstacle. Initiation de l'esquive...");

   
    float directionInitiale = deplacement.getDirection();

   
    while (capteurUltrason.getDistance() < 50) { 
        deplacement.tournerAsync(20); 
        Delay.msDelay(100); 
    }

    System.out.println("Chemin dégagé détecté. Avancement...");
    deplacement.stop();
    deplacement.avancer(30); 

    // Revenir à la trajectoire initiale
    float directionActuelle = deplacement.getDirection();
    float angleRetour = directionInitiale - directionActuelle;

  
    if (angleRetour > 180) {
        angleRetour -= 360;
    } else if (angleRetour < -180) {
        angleRetour += 360;
    }

    System.out.println("Retour à la trajectoire initiale.");
    deplacement.tournerAsync(angleRetour); 
    Delay.msDelay(500); 
}
    public static void main(String[] args) {
        Agent agent = new Agent();
        agent.run();
    }

}