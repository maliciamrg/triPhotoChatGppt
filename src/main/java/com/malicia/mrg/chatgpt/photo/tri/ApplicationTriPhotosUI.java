package com.malicia.mrg.chatgpt.photo.tri;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.ParseException;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.nio.file.Files;
import java.util.List;

class Photo {
    String nom;
    Date date;

    public Photo(String nom, Date date) {
        this.nom = nom;
        this.date = date;
    }
}

public class ApplicationTriPhotosUI {
    private JFrame frame;
    private JTextField sourceField;
    private JTextField destinationField;
    private JButton chooseSourceButton;
    private JButton chooseDestinationButton;
    private JButton trierButton;

    private JCheckBox dryRunCheckBox; // Nouveau

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new ApplicationTriPhotosUI().initialize();
            } catch (ParseException | IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void initialize() throws ParseException, IOException {
        frame = new JFrame("Tri de Photos");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new FlowLayout());

        sourceField = new JTextField(20);
        destinationField = new JTextField(20);
        chooseSourceButton = new JButton("Choisir source");
        chooseDestinationButton = new JButton("Choisir destination");
        trierButton = new JButton("Trier et Déplacer");
        dryRunCheckBox = new JCheckBox("Dry Run", true); // Coché par défaut

        sourceField.setText("P:\\00-CheckIn"); // Définir le répertoire source par défaut
        destinationField.setText("P:\\50-Phototheque\\50_Phototheque\\05-A_Ranger\\@New"); // Définir le répertoire de destination par défaut

        chooseSourceButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int result = chooser.showOpenDialog(frame);
                if (result == JFileChooser.APPROVE_OPTION) {
                    sourceField.setText(chooser.getSelectedFile().getAbsolutePath());
                }
            }
        });

        chooseDestinationButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int result = chooser.showOpenDialog(frame);
                if (result == JFileChooser.APPROVE_OPTION) {
                    destinationField.setText(chooser.getSelectedFile().getAbsolutePath());
                }
            }
        });

        trierButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    trierEtDeplacerPhotos();
                    JOptionPane.showMessageDialog(frame, "Photos triées déplacées avec succès.");
                } catch (ParseException | IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(frame, "Une erreur s'est produite.");
                }
            }
        });

        frame.add(new JLabel("Répertoire source des photos:"));
        frame.add(sourceField);
        frame.add(chooseSourceButton);
        frame.add(new JLabel("Répertoire de destination:"));
        frame.add(destinationField);
        frame.add(chooseDestinationButton);
        frame.add(trierButton);

        frame.add(dryRunCheckBox);
        frame.pack();
        frame.setVisible(true);
    }

    private void trierEtDeplacerPhotos() throws ParseException, IOException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        SimpleDateFormat dateFormatDestination = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

        File sourceFolder = new File(sourceField.getText());
        File destinationFolder = new File(destinationField.getText());

        if (!sourceFolder.isDirectory() || !destinationFolder.isDirectory()) {
            System.out.println("Veuillez spécifier des répertoires valides.");
            return;
        }

        File[] files = sourceFolder.listFiles();

        List<Photo> photos = new ArrayList<>();
        List<String> extensionsPermises = Arrays.asList(
                ".jpg", ".jpeg", ".png", ".gif", ".mp4", ".avi", ".arw", ".mts", ".mov");

        for (File file : files) {
            String fileExtension = getFileExtension(file).toLowerCase(); // Convertir l'extension en minuscules
            if (extensionsPermises.contains(fileExtension)) {
                photos.add(new Photo(file.getName(), new Date(file.lastModified())));
            }
        }

        Collections.sort(photos, Comparator.comparing(photo -> photo.date));

        List<List<Photo>> groupes = new ArrayList<>();
        List<Photo> groupeCourant = new ArrayList<>();
        groupeCourant.add(photos.get(0));

        for (int i = 1; i < photos.size(); i++) {
            long diffMillis = photos.get(i).date.getTime() - groupeCourant.get(0).date.getTime();
            if (diffMillis <= 2 * 60 * 60 * 1000) {
                groupeCourant.add(photos.get(i));
            } else {
                groupes.add(groupeCourant);
                groupeCourant = new ArrayList<>();
                groupeCourant.add(photos.get(i));
            }
        }

        groupes.add(groupeCourant);

        for (List<Photo> groupe : groupes) {

            // Créer le nom du sous-répertoire avec la date et l'heure du premier élément du groupe
            Date datePremierePhoto = groupe.get(0).date;
            String nomSousRepertoire = dateFormatDestination.format(datePremierePhoto);
            File sousRepertoire = new File(destinationFolder, nomSousRepertoire);

            if (!sousRepertoire.exists()) {
                sousRepertoire.mkdir();
            }

            for (Photo photo : groupe) {
                File sourceFile = new File(sourceFolder, photo.nom);

                // Obtenir la date et l'heure du fichier déplacé au format dateFormatDestination
                Date dateFichierDeplace = new Date(sourceFile.lastModified());
                String nomFichierDestination = dateFormatDestination.format(dateFichierDeplace) + "_" + photo.nom;

                File destinationFile = new File(sousRepertoire, nomFichierDestination);
                if (dryRunCheckBox.isSelected()) { // Vérifie si le dry run est activé
                    System.out.println("Dry Run: Déplacer " + sourceFile.getAbsolutePath() + " vers " + destinationFile.getAbsolutePath());
                } else {
                    Files.move(sourceFile.toPath(), destinationFile.toPath());
                }
            }
        }
    }
    private String getFileExtension(File file) {
        String name = file.getName();
        int lastDotIndex = name.lastIndexOf(".");
        if (lastDotIndex > 0) {
            return name.substring(lastDotIndex).toLowerCase(); // Convertir l'extension en minuscules
        }
        return "";
    }
}
