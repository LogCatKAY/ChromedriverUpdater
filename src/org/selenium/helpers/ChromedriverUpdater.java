package org.selenium.helpers;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Класс предназначен для проверки свежей версии cromedriver на сайте.
 * <br> https://sites.google.com/a/chromium.org/chromedriver/downloads
 * <p>
 *     Необходимо положить в проект в папку drivers файл chromedriverCurrentVersion.txt
 *     с номером версии используемой версии драйвера.
 *     <br>Например, вписать версию "2.41" без кавычек.
 * </p>
 * <p>
 *     Включает методы:
 *     <br>updateDriver() - выкачивает html страничку, парсит регуляркой номер версии, выкачивает архив
 *     и распаковывает его.
 * </p>
 */
public class ChromedriverUpdater {

    private static final String PATH_TO_FILE_WITH_VERSION = Paths.get("drivers/chromedriverCurrentVersion.txt").toString();
    private static final String URL_TO_DOWNLOADS = "https://sites.google.com/a/chromium.org/chromedriver/downloads";
    private static final String OS_NAME = System.getProperty("os.name");
    private static final String CHROMEDRIVER_FOLDER = "drivers";
    private static final String CHROMEDRIVER_FILE = "drivers" + File.separator + getChromedriverFileArchiveName(OS_NAME);

    private static double latestVersion;
    private static double currentVersion;
    private static String latestVersionURL;
    private static String latestVersionToReplace;
    private static boolean isUpdateCorrect = false;

    /**
     * Выбор имени архива для скачивания, в зависимости от ОС.
     * @return имя архива, который будет скачан с сайта ChromeDriver'а
     * */
    private static String getChromedriverFileArchiveName(String currentOS) {
        if(currentOS.contains("mac") || currentOS.contains("Mac"))
            return "chromedriver_mac64.zip";
        else if (currentOS.contains("windows") || currentOS.contains("Windows"))
            return "chromedriver_win32.zip";
        else
            return "chromedriver_linux64.zip";
    }

    /**
     *  Инициализация необходимых ждя работы программы файлов и папок, если они не были созданы ранее.<br>
     *  Если не было создано директории drivers - создается в корне (места, откуда программа запущена).<br>
     *  Если не был создан файл с версией драйвера - создается с последней имеющейся на сайте версией драйвера.
     * @return true - если файл был создан методом, false - если файл был уже подготовлен заранее
     * */
    private static boolean initFilesToStart(String versionForDownload) {
        File driversFolder = new File(CHROMEDRIVER_FOLDER);
        File chromedriverVersionFile = new File(PATH_TO_FILE_WITH_VERSION);

        if (!driversFolder.exists())
            driversFolder.mkdir();

        if (!chromedriverVersionFile.exists() && !chromedriverVersionFile.isDirectory()) {
            writeNewVersionInFile(versionForDownload);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Основной метод класса.
     * <br>
     * Выкачивает html страничку https://sites.google.com/a/chromium.org/chromedriver/downloads,
     * парсит регуляркой номер версии, выкачивает архив и распаковывает его в папку drivers.
     * <br>Выкачивается драйвер для ОС, на которой запускалась программа.
     */
    public static void updateDriver(){

        latestVersion = getLatestVersion();

        if (!initFilesToStart(String.valueOf(latestVersion)))
            currentVersion = getCurrentVersion();
        else
            currentVersion = 0.0;

        latestVersionToReplace = String.valueOf(latestVersion);

        if(OS_NAME.contains("mac") || OS_NAME.contains("Mac"))
            latestVersionURL = String.format("https://chromedriver.storage.googleapis.com/%s/chromedriver_mac64.zip", latestVersionToReplace);
        else if (OS_NAME.contains("windows") || OS_NAME.contains("Windows"))
            latestVersionURL = String.format("https://chromedriver.storage.googleapis.com/%s/chromedriver_win32.zip", latestVersionToReplace);
        else
            latestVersionURL = String.format("https://chromedriver.storage.googleapis.com/%s/chromedriver_linux64.zip", latestVersionToReplace);

        if(latestVersion <= currentVersion) {
            return;
        }

        isUpdateCorrect = downloadNewDriver(latestVersionURL);

        if(!isUpdateCorrect){
            System.out.println("Download new version failed");
            return;
        } else {
            isUpdateCorrect = unZipIt(CHROMEDRIVER_FILE, CHROMEDRIVER_FOLDER);
        }

        if(isUpdateCorrect) {
            writeNewVersionInFile(latestVersionToReplace);
        } else {
            System.out.println("Write new version in the file failed");
            return;
        }
    }

    /**
     * Выкачивает html страничку через метод savePage(URL) внутри,
     * непосредственно парсит регуляркой номер версии того, что выкачал.
     * @return double номер версии
     */
    private static double getLatestVersion() {
        double latestVersion;
        String savedPageHtml = "";
        try {
            savedPageHtml = savePage(URL_TO_DOWNLOADS);

        } catch (IOException ex){
            ex.printStackTrace();
        }
        String temp = "";
        String res = "";

        //2 цикла для регулярки, т.к. пока непонятно, как сделать проще
        //1й цикл ищется строка вида "Latest-Release:-ChromeDriver-2.41"
        //2й цикл ищется строка вида "2.41"
        Matcher matcher = Pattern.compile("(Latest-Release:-ChromeDriver-)([0-9]+\\S[0-9]+)[^\"]").matcher(savedPageHtml);
        while (matcher.find())
            res = matcher.group();
        temp = res;

        matcher = Pattern.compile("[0-9]+\\S[0-9]+").matcher(temp);
        while (matcher.find())
            res = matcher.group();
        String version = res;

        latestVersion = Double.parseDouble(version);
        return latestVersion;
    }

    /**
     * Выкачивает html страничку https://sites.google.com/a/chromium.org/chromedriver/downloads,
     * чтобы можно было, например, выполнить парсинг по разметке.
     * @param URL страничка, которую надо скачать
     * @return String html разметки
     * @throws IOException
     */
    private static String savePage(final String URL) throws IOException {
        String line = "", all = "";
        URL myUrl = null;
        BufferedReader in = null;
        try {
            myUrl = new URL(URL);
            in = new BufferedReader(new InputStreamReader(myUrl.openStream()));

            while ((line = in.readLine()) != null) {
                all += line;
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }

        return all;
    }

    /**
     * Открывает файл:
     * <blockquote><pre>drivers/chromedriverCurrentVersion.txt</pre></blockquote>
     * Парсит номер версии в формате "2.41" и возвращает её в формате double.
     * @return double номер версии используемого chromedriver
     */
    private static double getCurrentVersion(){
        double currentVersion;
        ArrayList<Double> versionFromFile = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(PATH_TO_FILE_WITH_VERSION)))
        {

            String temp;

            while ((temp = reader.readLine()) != null) {
                versionFromFile.add(Double.parseDouble(temp));
            }
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

        currentVersion = versionFromFile.get(0);

        return currentVersion;
    }

    /**
     * Открывает подключение к ссылке на скачивание новой версии. Качает драйвер (обычно архив).
     * @param url откуда качать
     * @return boolean флаг успешности
     */
    private static boolean downloadNewDriver(String url){
        try {
            URL website = new URL(url);
            ReadableByteChannel rbc = Channels.newChannel(website.openStream());
            FileOutputStream fos = new FileOutputStream(CHROMEDRIVER_FILE);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            fos.close();
            rbc.close();

            return true;
        } catch (IOException e) {
            e.printStackTrace();

            return false;
        }
    }

    /**
     * Распаковывает zip фрхивы.
     * @param zipFile путь к файлу для распаковки (например, drivers/chromedriver_mac64.zip).
     * @param outputFolder путь к директории для извлечения (например, drivers).
     * @return boolean флаг успешности.
     */
    private static boolean unZipIt(String zipFile, String outputFolder){

        byte[] buffer = new byte[1024];

        try{

            //create output directory is not exists
            File folder = new File(outputFolder);
            if(!folder.exists()){
                folder.mkdir();
            }

            //get the zip file content
            ZipInputStream zis =
                    new ZipInputStream(new FileInputStream(zipFile));
            //get the zipped file list entry
            ZipEntry ze = zis.getNextEntry();

            while(ze != null){

                String fileName = ze.getName();
                File newFile = new File(outputFolder + File.separator + fileName);

                //create all non exists folders
                //else you will hit FileNotFoundException for compressed folder
                new File(newFile.getParent()).mkdirs();

                FileOutputStream fos = new FileOutputStream(newFile);

                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }

                fos.close();
                ze = zis.getNextEntry();

                if(!CHROMEDRIVER_FILE.contains("chromedriver_win32.zip"))
                    Runtime.getRuntime().exec("chmod +x " + outputFolder + File.separator + fileName);
            }

            zis.closeEntry();
            zis.close();

            return true;

        }catch(IOException ex){
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * Выполняет обновление файла:
     * <pre>./drivers/chromedriverCurrentVersion.txt</pre>
     * @param newVersion новая версия скачанного драйвера для записи в файл
     */
    private static void writeNewVersionInFile(String newVersion){
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(PATH_TO_FILE_WITH_VERSION)))
        {
            bw.write(newVersion);
        }
        catch(IOException ioe){
            ioe.printStackTrace();
        }
    }
}
