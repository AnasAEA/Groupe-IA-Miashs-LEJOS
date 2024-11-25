package perception;
import java.util.ArrayList;
import action.Deplacement;
import lejos.hardware.port.Port;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.robotics.SampleProvider;

public class CapteurUltrason {
	Float distanceActuelle = Float.MAX_VALUE; //distance actuelle
	Float distancePrecedente = Float.MAX_VALUE; //la derniére distance qui a été captée avant celle actuelle
	EV3UltrasonicSensor ultrasonicSensor;  //instance de la classe prédéfinie EV3UltrasonicSensor
    	SampleProvider distanceProvider; //echantillon de distance donné par le capteur ultrason
    	float[] samples;  //tableau qui stocke les distances.	
    	Deplacement deplacement;
    	ArrayList <float[][]> liste;

	public CapteurUltrason(Port sensorPort) {   //initialisation de l'attribut sensor port qui permet l'accÃ©s au moteur
		this.ultrasonicSensor = new EV3UltrasonicSensor(sensorPort);
		this.distanceProvider = ultrasonicSensor.getDistanceMode();
		this.samples = new float[distanceProvider.sampleSize()]; 
		deplacement = new Deplacement();
		liste = new ArrayList <float[][]>();
	}

		

	public float getDistance() {                  //récupére la distance captée par l'ultrason et la stocke dans le tableau aprÃ¨s une conversion en cm
	        distanceProvider.fetchSample(samples, 0); 
	        float distanceCm = samples[0] * 100; 
	        return distanceCm; 
    	}

	
	public boolean detecterPalet() {
	    float distanceMinDetection = 32.6f; // La distance à laquelle nous prévoyons de détecter le palet
	    float tolerance = 1.0f; // Tolérance pour les lectures du capteur
	    boolean distanceMinAtteinte = false;
	    //Initialization 
	    float distanceActuelle = this.getDistance();
	    float distancePrecedente = distanceActuelle;
	   // Veifiier si le robot est trop proche de l'objet ( <detectionMin)
	    if (distanceActuelle <= distanceMinDetection - tolerance) {
	        deplacement.stop();
	        System.out.println("Already too close to the object without detecting the pallet.");
	        return false; // Not a pallet
	    }
	  //Verifier si la distance Minimale est atteinte + tolerance
	   if (distanceActuelle <= distanceMinDetection + tolerance) {
	        distanceMinAtteinte = true;
	        System.out.println("Minimal detection distance already reached.");
	    }
	   // commencer d'avancer uniquement une fois distMinAtteinte
	   if (!distanceMinAtteinte) {
	        deplacement.modifVitLin(25); // Set linear speed to 25
	        deplacement.avancerSync(distanceActuelle);
	    }
		
	    while (deplacement.isMoving()) {
	        distanceActuelle = this.getDistance();
	        System.out.println("Distance actuelle : " + distanceActuelle + " cm");
	        // Vérifier si nous avons atteint la distance minimale de détection
	        if (!distanceMinAtteinte && distanceActuelle <= distanceMinDetection + tolerance) {
	            distanceMinAtteinte = true;
	            System.out.println("Distance minimale de détection atteinte.");
		}
	        // Après avoir atteint la distance minimale, vérifier si la distance augmente
	        else if (distanceMinAtteinte) {
	            if (distanceActuelle > distancePrecedente + tolerance) {
	                // La distance a augmenté après la distance minimale
	                deplacement.stop();
	                System.out.println("La distance a augmenté après la distance minimale de détection.");
	                return true; // Palet détecté
	            } else if (distanceActuelle < distanceMinDetection - tolerance) {
	                // Nous nous rapprochons trop sans détecter le palet
	                deplacement.stop();
	                System.out.println("Trop proche de l'objet sans détecter le palet.");
	                return false; // Pas un palet
	            }
	        }
		    //Mettre a jour la distance
	        distancePrecedente = distanceActuelle;
	        if (!deplacement.getPilot().isMoving()) {
	            // Le robot a cessé de bouger
	            break;
	        }
	        // Pause pour éviter une boucle trop rapide
	        try {
	            Thread.sleep(50);
	        } catch (InterruptedException e) {
	            e.printStackTrace();
	        }
	    }
	    // Valeur de retour par défaut si aucun palet n'est détecté
	    deplacement.stop();
	    return false;
    }

     public boolean detectObjet(float detectionMin) {
        float distanceDobjet = this.getDistance();
        return distanceDobjet < detectionMin;
    }

    public void close() {
        this.close();
    }
}

