package com.example.dpoae;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.io.input.ReversedLinesFileReader;

public class FileOperations {

    public static short[] readrawasset_binary(Context context, int id) {
        InputStream inp = context.getResources().openRawResource(id);
        ArrayList<Integer> ll = new ArrayList<>();
        int counter=0;
        int byteRead=0;
        try {
            while ((byteRead = inp.read()) != -1) {
                ll.add(byteRead);
                counter += 1;
//                if (counter % 1000 == 0) {
//                    Log.e("asdf", counter + "");
//                }
            }
            inp.close();
        }
        catch(Exception e) {
            Log.e("asdf",e.getMessage());
        }
        short[] ar = new short[ll.size()/2];

        counter=0;
        for (int i = 0; i < ll.size(); i+=2) {
            int out=ll.get(i)+ll.get(i+1)*256;
            if (out > 32767) {
                out=out-65536;
            }
            ar[counter++]=(short)out;
        }

        return ar;
    }

    public static double[] readrawasset(Context context, int id) {

        Scanner inp = new Scanner(context.getResources().openRawResource(id));
        LinkedList<Double> ll = new LinkedList<Double>();
        while (inp.hasNextLine()) {
            ll.add(Double.parseDouble(inp.nextLine()));
        }
        inp.close();
        double[] ar = new double[ll.size()];
        int counter = 0;
        for (Double d : ll) {
            ar[counter++] = d;
        }
        ll.clear();

        return ar;
    }

    public static double[] readfromfile(Activity av, String filename) {
        LinkedList<Double> ll = new LinkedList<Double>();

        try {
            String dir = av.getExternalFilesDir(null).toString();
            File file = new File(dir + File.separator + filename);
            BufferedReader buf = new BufferedReader(new FileReader(file));

            String line;
            while ((line = buf.readLine()) != null && line.length() != 0) {
                ll.add(Double.parseDouble(line));
            }

            buf.close();
        } catch (Exception e) {
            Log.e("ble",e.getMessage());
        }

        double[] ar = new double[ll.size()];
        int counter = 0;
        for (Double d : ll) {
            ar[counter++] = d;
        }
        ll.clear();
        return ar;
    }

    public static short[] readfromfile_short(Activity av, String filename) {
        LinkedList<Short> ll = new LinkedList<>();

        try {
            String dir = av.getExternalFilesDir(null).toString();
            File file = new File(dir + File.separator + filename);
            Log.e("exists",file.getAbsoluteFile()+","+file.exists());
            BufferedReader buf = new BufferedReader(new FileReader(file));

            String line;
            while ((line = buf.readLine()) != null && line.length() != 0) {
                ll.add(Short.parseShort(line));
            }

            buf.close();
        } catch (Exception e) {
            Log.e("ble",e.getMessage());
        }

        short[] ar = new short[ll.size()];
        int counter = 0;
        for (Short d : ll) {
            ar[counter++] = d;
        }
        ll.clear();
        return ar;
    }

    public static String[] getLastNLinesFromFile(String filePath, int numLines) throws IOException {
        try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
            AtomicInteger offset = new AtomicInteger();
            String[] lines = new String[numLines];
            stream.forEach(line -> {
                lines[offset.getAndIncrement() % numLines] = line;
            });
            List<String> list = IntStream.range(offset.get() < numLines ? 0 : offset.get() - numLines, offset.get())
                    .mapToObj(idx -> lines[idx % numLines]).collect(Collectors.toList());
            return list.toArray(new String[0]);
        }
    }

    public static String getAttemptNumber(Activity av,String pid, String uid, String ear,long tt) {
        Log.e("file","get attempt");
        File file = new File(av.getExternalFilesDir(null).toString() + File.separator + "file_list.txt");
        if (!file.exists()) {
            Log.e("file","return");
            return "1";
        }

        long oneHourInMillis = 60 * 60 * 1000;
        try {
            int n_lines = 100;

            String[] lines = getLastNLinesFromFile(av.getExternalFilesDir(null).toString() + File.separator + "file_list.txt",n_lines);
            LinkedList<Integer>files=new LinkedList<>();
            for (String line : lines) {
                if (line != null) {
                    String[] elts = line.split("\t");
                    long timestamp = Long.parseLong(elts[1]);
                    if (tt - timestamp <= oneHourInMillis && elts[0] != null) {
                        String[] elts2 = elts[0].split("-");
                        if (elts2.length >= 4) {
                            String lastPid = elts2[3];
                            String lastUid = elts2[4];
                            String lastEar = elts2[5];
                            String lastAttempt = elts2[6];
                            if (lastPid.equals(pid) && lastUid.equals(uid) && lastEar.equals(ear)) {
//                                Log.e("file", "here " + lastAttempt+"\t"+elts[0]);
                                files.add(Integer.parseInt(lastAttempt));
                            }
                        }
                    }
                }
            }

            int maxAttempt = 0;
            for (Integer i : files) {
                if (i>maxAttempt){
                    maxAttempt=i;
                }
            }
            Log.e("file","return here "+(maxAttempt+1));
            return ""+(maxAttempt+1);
        }
        catch(Exception e) {
            Log.e("file",e.getMessage());
            return "1";
        }
    }

    public static void writeCSV(Activity av,String pid, String uid, String ear, String attemptNumber, double[] sig,double[]noise,double[]snr,String noiseVal,double[]ambient,boolean result,int toneLength, boolean test_complete) {
        try {
            Log.e("asdf","writnig rec to disk");
            String dir = av.getExternalFilesDir(null).toString();
            File path = new File(dir);
            if (!path.exists()) {
                path.mkdirs();
            }

            File file0 = new File(dir, "file_list.txt");
            if (!file0.exists()) {
                file0.createNewFile();
            }
            BufferedWriter outfile0 = new BufferedWriter(new FileWriter(file0,true));

            outfile0.append(Constants.filename+"\t"+System.currentTimeMillis()+"\n");
            outfile0.close();

            File file = new File(dir, Constants.filename+"-summary.csv");
            BufferedWriter outfile = new BufferedWriter(new FileWriter(file,false));
            outfile.append("patient_id,user_id,ear,attempt_id,site,");
            outfile.append("f1_2khz,f1_3khz,f1_4khz,f1_5khz,f2_2khz,f2_3khz,f2_4khz,f2_5khz,oae_2khz,oae_3khz,oae_4khz,oae_5khz,");
            outfile.append("2khz_selected,3khz_selected,4khz_selected,5khz_selected,");

            outfile.append("2khz_f1_sig,3khz_f1_sig,4khz_f1_sig,5khz_f1_sig,");
            outfile.append("2khz_f1_noise,3khz_f1_noise,4khz_f1_noise,5khz_f1_noise,");
            outfile.append("2khz_f1_snr,3khz_f1_snr,4khz_f1_snr,5khz_f1_snr,");
            outfile.append("2khz_f1_snr_result,3khz_f1_snr_result,4khz_f1_snr_result,5khz_f1_snr_result,");
            outfile.append("2khz_f2_sig,3khz_f2_sig,4khz_f2_sig,5khz_f2_sig,");
            outfile.append("2khz_f2_noise,3khz_f2_noise,4khz_f2_noise,5khz_f2_noise,");
            outfile.append("2khz_f2_snr,3khz_f2_snr,4khz_f2_snr,5khz_f2_snr,");
            outfile.append("2khz_f2_snr_result,3khz_f2_snr_result,4khz_f2_snr_result,5khz_f2_snr_result,");

            outfile.append("sig_2khz,sig_3khz,sig_4khz,sig_5khz,noise_2khz,noise_3khz,noise_4khz,noise_5khz,snr_2khz,snr_3khz,snr_4khz,snr_5khz,");
            outfile.append("2khz_result,3khz_result,4khz_result,5khz_result,");
            outfile.append("test_result,test_complete,ambient_noise_2khz,ambient_noise_3khz,ambient_noise_4khz,ambient_noise_5khz,");
            outfile.append("ambient_noise_result_2khz,ambient_noise_result_3khz,ambient_noise_result_4khz,ambient_noise_result_5khz,ambient_noise_result,tone_length,");
            outfile.append("check_fit_val,check_fit_thresh,year,month,snr_thresh1,snr_thresh2,snr_thresh3,snr_thresh4,band_thresh\n");

            outfile.write(pid+","+uid+","+ear+","+attemptNumber+","+Constants.site+",");
            for(int i = 0; i < Constants.f1.length; i++) {
                outfile.append(Constants.f1[i]+",");
            }
            for(int i = 0; i < Constants.f2.length; i++) {
                outfile.append(Constants.f2[i]+",");
            }
            for(int i = 0; i < Constants.oaes.length; i++) {
                outfile.append(Constants.oaes[i]+",");
            }
            for (Boolean b : Constants.freqs) {
                outfile.append(b+",");
            }
            for (Double i : Constants.signal_f1) {
                outfile.append(i+",");
            }
            for (Double i : Constants.noise_f1) {
                outfile.append(i+",");
            }
            for (Double i : Constants.snrs_f1) {
                outfile.append(i+",");
            }
            for (Double i : Constants.snrs_f1) {
                if (i<Constants.TONE_F1_MIN_THRESH) {
                    outfile.append("false,");
                }
                else {
                    outfile.append("true,");
                }
            }
            for (Double i : Constants.signal_f2) {
                outfile.append(i+",");
            }
            for (Double i : Constants.noise_f2) {
                outfile.append(i+",");
            }
            for (Double i : Constants.snrs_f2) {
                outfile.append(i+",");
            }
            for (Double i : Constants.snrs_f2) {
                if (i<Constants.TONE_F2_MIN_THRESH) {
                    outfile.append("false,");
                }
                else {
                    outfile.append("true,");
                }
            }
            for (Double i : sig) {
                outfile.append(i+",");
            }
            for (Double i : noise) {
                outfile.append(i+",");
            }
            for (Double i : snr) {
                outfile.append(i+",");
            }
            int counter=0;
            for (Double i : snr) {
                if (i >= Constants.SNR_THRESHS[counter]) {
                    outfile.append("true,");
                }
                else {
                    outfile.append("false,");
                }
                counter+=1;
            }
            if (result) {
                outfile.append("Pass,");
            }
            else {
                outfile.append("Refer,");
            }
            outfile.write(test_complete+",");
            for (Double i : ambient) {
                outfile.append(i+",");
            }
            outfile.append(noiseVal+",");
            outfile.append(toneLength+",");
            outfile.append(Constants.CheckFitProgress+","+Constants.SEAL_CHECK_THRESH+",");
            outfile.append(Constants.patientYear+","+Constants.patientMonth+",");
            outfile.append(Constants.SNR_THRESHS[0]+","+Constants.SNR_THRESHS[1]+","+Constants.SNR_THRESHS[2]+","+Constants.SNR_THRESHS[3]+","+Constants.BAND_PASS_THRESH);
            outfile.flush();
            outfile.close();
            Log.e("ex", "writeCSVToDisk");
        } catch(Exception e) {
            Log.e("ex", "writeRecToDisk");
            Log.e("ex", e.getMessage());
        }
    }

    public static void writeFileToDisk(Activity av,LinkedList<Integer>envs) {
        try {
            String dir = av.getExternalFilesDir(null).toString();
            File path = new File(dir);
            if (!path.exists()) {
                path.mkdirs();
            }
            File file = new File(dir, Constants.filename+"-checkfit.txt");
            BufferedWriter outfile = new BufferedWriter(new FileWriter(file,false));
            for (Integer i : envs) {
                outfile.append("" + i);
                outfile.newLine();
            }
            outfile.flush();
            outfile.close();
        } catch(Exception e) {
            Log.e("ex", "writeRecToDisk");
            Log.e("ex", e.getMessage());
        }
    }

    public static void writeFileToDisk(Activity av,short[]envs,boolean checkfit,boolean calib) {
        try {
            String dir = av.getExternalFilesDir(null).toString();
            File path = new File(dir);
            if (!path.exists()) {
                path.mkdirs();
            }
            String filename=Constants.filename;
            if (calib) {
                filename+="-volcalib.txt";
            }
            else if (checkfit) {
                filename+="-env2.txt";
            }
            Log.e("asdf","write file "+filename);

            File file = new File(dir, filename);
            BufferedWriter outfile = new BufferedWriter(new FileWriter(file,false));
            for (short i : envs) {
                outfile.append("" + i);
                outfile.newLine();
            }
            outfile.flush();
            outfile.close();
            Log.e("asdf","finish write file "+filename);
        } catch(Exception e) {
            Log.e("ex", "writeRecToDisk");
            Log.e("ex", e.getMessage());
        }
    }

    public static void writeRecToDisk(Activity av, TextView fnameView) {
        try {
            String dir = av.getExternalFilesDir(null).toString();
            File path = new File(dir);
            if (!path.exists()) {
                path.mkdirs();
            }

            File file = new File(dir, Constants.filename+".txt");

            BufferedWriter outfile = new BufferedWriter(new FileWriter(file,false));
            int cc = 1;
            for (short[] buff : Constants.fullrec) {
                Log.e("out","writing "+cc+" out of "+Constants.fullrec.size());
                for (int i = 0; i < buff.length; i++) {
                    outfile.append("" + buff[i]);
                    outfile.newLine();
                }
                cc+=1;
            }
            outfile.flush();
            outfile.close();
        } catch(Exception e) {
            Log.e("ex", "writeRecToDisk");
            Log.e("ex", e.getMessage());
        }
    }
}
