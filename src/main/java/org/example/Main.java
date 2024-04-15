package org.example;
import org.apache.commons.lang3.tuple.Pair;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) {
//        if (args.length != 2) {
//            System.err.println("Usage: java ImageProcessingPipeline <inputDirectory> <outputDirectory>");
//            System.exit(1);
//        }
//
//        String inputDirectory = args[0];
//        String outputDirectory = args[1];
        String inputDirectory = "src/main/resources/sourceImage";
        String outputDirectory = "src/main/resources/destImage";
        List<Pair<String, BufferedImage>> images = loadImages(inputDirectory);


        if (images.isEmpty()) {
            System.err.println("No images found in the input directory.");
            System.exit(1);
        }

        //ExecutorService executor = Executors.newFixedThreadPool(4); //Conclusion -> The optimal thread count is around 4-5 threads. Less and more will result it longer execution time

        String finalOutputDirectory = outputDirectory;
        ForkJoinPool customThreadPool = new ForkJoinPool(6);
        long startTime = System.currentTimeMillis();

        try {
            customThreadPool.submit(
                    () ->
                            images.parallelStream()
                                    .map(pair -> Pair.of(pair.getLeft(), processImage(pair.getRight())))
                                    .forEach(pair -> saveImage(pair.getRight(), finalOutputDirectory, pair.getLeft()))).get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        System.out.println("Total execution time: " + totalTime + " milliseconds");
        customThreadPool.shutdown();
        //executor.shutdown();
    }

    private static List<Pair<String, BufferedImage>> loadImages(String inputDirectory) {
        try {
            return Files.list(Path.of(inputDirectory))
                    .parallel()
                    .map(path -> {
                        try {
                            BufferedImage image = ImageIO.read(path.toFile());
                            return Pair.of(path.getFileName().toString(), image);
                        } catch (IOException e) {
                            e.printStackTrace();
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    private static BufferedImage processImage(BufferedImage original) {
        BufferedImage processed = new BufferedImage(original.getWidth(), original.getHeight(), original.getType());

        for (int i = 0; i < original.getWidth(); i++) {
            for (int j = 0; j < original.getHeight(); j++) {
                int rgb = original.getRGB(i, j);
                Color color = new Color(rgb);
                int red = color.getRed();
                int blue = color.getBlue();
                int green = color.getGreen();
                //Color newColor = new Color(saturated(green - 50),saturated(red - 50), saturated(blue - 50)); // swap green with red
                Color newColor = new Color(negative(red), negative(green), negative(blue));
                processed.setRGB(i, j, newColor.getRGB());
            }
        }

        return processed;
    }

    private static void saveImage(BufferedImage image, String outputDirectory, String fileName) {
        try {
            Path outputPath = Path.of(outputDirectory, fileName);
            ImageIO.write(image, "jpg", outputPath.toFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static int saturated(int value) {
        if (value > 255) {
            return 255;
        }
        else if (value < 0) {
            return 0;
        }
        return value;
    }
    private static int negative(int value) {
        return Math.abs(255 - value);
    }
}
