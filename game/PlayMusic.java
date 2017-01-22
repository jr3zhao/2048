import java.io.*;
import javax.sound.sampled.*;
 
public class PlayMusic {
    static volatile boolean stop=false;
    public static void main(String[] args) {
        PlayMusic.Play("game/beep.wav");
    }
 
    public static void Play(String file) {
      while(true){
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(new File(file));
            AudioFormat aif = ais.getFormat();
            final SourceDataLine sdl;
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, aif);
            sdl = (SourceDataLine) AudioSystem.getLine(info);
            sdl.open(aif);
            sdl.start();
            FloatControl fc=(FloatControl)sdl.getControl(FloatControl.Type.MASTER_GAIN);
            double value=2;
            float dB = (float)
                  (Math.log(value==0.0?0.0001:value)/Math.log(10.0)*20.0);
            fc.setValue(dB);
            int nByte = 0;
            int writeByte = 0;
            final int SIZE=1024*64;
            byte[] buffer = new byte[SIZE];
            while (nByte != -1) {
                nByte = ais.read(buffer, 0, SIZE);
                sdl.write(buffer, 0, nByte);
            }
            sdl.stop();
 
        } catch (Exception e) {
            e.printStackTrace();
        }
        }
    }
}