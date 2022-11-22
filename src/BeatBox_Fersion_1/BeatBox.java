package BeatBox_Fersion_1;

import javax.sound.midi.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;

public class BeatBox {
    JPanel mainPanel;
    ArrayList<JCheckBox> checkboxList;// Мы храним флажки в массиве ArrayList.
    Sequencer sequencer;
    Sequence sequence;
    Track track;
    JFrame theFrame;
    // Это названия инструментов в виде строкового массива, предназначенные для создания меток в пользовательском интерфейсе(на каждый ряд).
    String[] instrumentalNames = {"Bass Drum", "Closed Hi-Hat", "Open Hi-Hat", "Acoustic Snare", "Crash Cymbal", "Hand Clap",
            "High Tom", "Hi Bongo", "Maracas", "Whistle", "Low Conga", "Cowbell", "Vibraslap", "Low-mid Tom", "High Agogo", "Open Hi Conga"};
    int[] instruments = {35, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58, 47, 67, 63};// Эти числа представляют собой фактические барабанные клавиши.
    //Канал барабана-это что-то вроде фортепиано, только каждая клавиша на нем-отдельный барабан. Номер 35-это клавиша для Bass Drum, а 42-Closed Hi-Hat и т.д.

    public static void main(String[] args) {
        new BeatBox().buildGUI();
    }

    public void buildGUI() {
        theFrame = new JFrame("Ceber BeatBox");
        theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        BorderLayout layout = new BorderLayout();
        JPanel background = new JPanel(layout);
        background.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));// Пустая граница позволяет создать поля между краями
        //панели и местом размещения компонентов.

        checkboxList = new ArrayList<JCheckBox>();
        Box buttonBox = new Box(BoxLayout.Y_AXIS);

        JButton start = new JButton("Start");
        start.addActionListener(new MyStartListener());
        buttonBox.add(start);

        JButton stop = new JButton("Stop");
        stop.addActionListener(new MyStopListener());
        buttonBox.add(stop);

        JButton upTempo = new JButton("Tempo up");
        upTempo.addActionListener(new MyUpTempoListener());
        buttonBox.add(upTempo);

        JButton downTempo = new JButton("Tempo down");
        downTempo.addActionListener((new MyDownTempoListener()));
        buttonBox.add(downTempo);

        JButton serializelt = new JButton("Serializelt");
        serializelt.addActionListener(new MySendListener());
        buttonBox.add(serializelt);

        JButton restore = new JButton("Restore");
        restore.addActionListener(new MyReadListener());
        buttonBox.add(restore);

        Box nameBox = new Box(BoxLayout.Y_AXIS);
        for (int i = 0; i < 16; i++) {
            nameBox.add(new Label(instrumentalNames[i]));
        }
        background.add(BorderLayout.EAST, buttonBox);
        background.add(BorderLayout.WEST, nameBox);

        theFrame.getContentPane().add(background);

        GridLayout grid = new GridLayout(16, 16);
        grid.setVgap(1);
        grid.setHgap(2);
        mainPanel = new JPanel(grid);
        background.add(BorderLayout.CENTER, mainPanel);

        for (int i = 0; i < 256; i++) {// Создаём флажки, присваиваем им значения false(чтобы они не были установлены), а затем
            JCheckBox c = new JCheckBox();//добавляем их в массив ArrayList и на панель.
            c.setSelected(false);
            checkboxList.add(c);
            mainPanel.add(c);
        }

        setUpMidi();

        theFrame.setBounds(50, 50, 300, 300);
        theFrame.pack();
        theFrame.setVisible(true);
    }

    public void setUpMidi() {// Обычный MIDI-код для получения синтезатора, секвенсора и дорожки. По-прежнему ничего особенного.
        try {
            sequencer = MidiSystem.getSequencer();
            sequencer.open();
            sequence = new Sequence(Sequence.PPQ, 4);
            track = sequence.createTrack();
            sequencer.setTempoInBPM(120);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Вот здесь всё и происходит! Мы преобразуем состояния флажков в MIDI- события и добавляем их на дорожку.
    public void buildTrackAndStart() {// Создаём массив из 16 элементов, чтобы хранить значения для каждого инструмента на все 16 тактов.
        int[] trackList = null;

        sequence.deleteTrack(track);// Избавляемся от старой дорожки и создаём новую.
        track = sequence.createTrack();


        for (int i = 0; i < 14; i++) {// Делаем это для каждого из 16 рядов(то есть для Bass, Congo и т.д.
            trackList = new int[16];

            int key = instruments[i];// Задаём клавишу, которая представляет инструмент(Bass, Hi-Hat и т.д.).
            // Массив содержит MIDI-числа для каждого инструмента.

            for (int j = 0; j < 16; j++) {// Делаем это для каждого такта текущего ряда.

                JCheckBox jc = (JCheckBox) checkboxList.get(j + (16 * i));
                if (jc.isSelected()) {// Установлен лы флажок на этом такте? Если до, то помещаем значение клавиши в текущую ячейку массива(ячейку, которая
                    trackList[j] = key;//представляет такт). Если нет, то инструмент не должен играть в этом такте, поэтому присваиваем ему 0.
                } else {
                    trackList[j] = 0;
                }
            }

            makeTracks(trackList);// Для всего инструмента и для всех 16 тактов создаём события и добавляем их на дорожку.
            track.add(makeEvent(176, 1, 127, 0, 16));
        }

        track.add(makeEvent(192, 9, 1, 0, 15));// Мы всегда должны быть уверены, что событие на такте 16 существует
        try {//(они идут от 0 до 15). Иначе BeatBox может не пройти все 16 тактов, перед тем как заново начнёт последовательность.

            sequencer.setSequence(sequence);
            sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);// Позволяет задать количество повторений цикла или, как в этом случае, непрерывный цикл.
            sequencer.start();// Теперь мы проигрываем мелодию.
            sequencer.setTempoInBPM(120);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class MyStartListener implements ActionListener {// Первый виз внутренних классов.

        public void actionPerformed(ActionEvent a) {
            buildTrackAndStart();
        }
    }


    // ДРУГИЕ ВНУТРЕННИЕ КОЛАССЫ-СЛУШАТЕЛИ ДЛЯ КНОПОК.
    public class MyStopListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            sequencer.stop();
        }
    }

    public class MyUpTempoListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            float tempoFactor = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float) (tempoFactor * 1.03));// Коэффициент темпа определяет темп синтезатора. По умолчанию он равен 1.0,
        }
    }

    public class MyDownTempoListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            float tempoFactor = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float) (tempoFactor * 0.97));//поэтому щелчком кнопкой мыши можно изменить его на +/- 3%.
        }
    }

    public void makeTracks(int[] list) {// Метод создаём события для одного инструмента за каждый проход цикла для всех 16 тактов. Можно
        //получить int[] для Bass drum, и каждый элемент массива будет содержать либо клавишу этого инструмента, либо ноль.
        //Если ноль, то инструмент не должен играть на текущем такте. Иначе нужно создать событие и добавить его в дорожку.
        for (int i = 0; i < 16; i++) {
            int key = list[i];

            if (key != 0) {// Создаём события включения и выключения и добавляем их в дорожку.
                track.add(makeEvent(144, 9, key, 100, i));
                track.add(makeEvent(128, 9, key, 100, i + 1));
            }
        }
    }

    public MidiEvent makeEvent(int comd, int chan, int one, int two, int trick) {// Это полезный метод
        MidiEvent event = null;//из предыдущей главы Кухни кода MiniMusicPlayer1.
        try {
            ShortMessage a = new ShortMessage();
            a.setMessage(comd, chan, one, two);
            event = new MidiEvent(a, trick);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return event;
    }

    public class MySendListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {// Все это происходит при нажатии кнопки, после чего срабатывает ActionEvent.

            boolean[] checkboxState = new boolean[256];// Создаём булев массив для хранения состояния каждого флажка.

            for (int i = 0; i < 256; i++) {// Пробегаем через checkboxList(ArrayList, содержащий состояния флажков), считываем
                JCheckBox check = (JCheckBox) checkboxList.get(i);//состояния и добавляем полученные значения в булев массив.
                if (check.isSelected()) {
                    checkboxState[i] = true;
                }
            }
            try {// Это самая простая часть. Просто запишите/сериализуйте булев массив!
                FileOutputStream fileStream = new FileOutputStream(new File("Checkbox.ser"));
                ObjectOutputStream os = new ObjectOutputStream(fileStream);
                os.writeObject(checkboxState);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public class MyReadListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            boolean[] checkboxState = null;
            try {
                FileInputStream fileIn = new FileInputStream(new File("Checkbox.ser"));
                ObjectInputStream is = new ObjectInputStream(fileIn);
                checkboxState = (boolean[]) is.readObject();// Считываем объект из файла и определяем его как булев массив(помните, readObject()
                //возвращает ссылку на тип Object).
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            for (int i = 0; i < 256; i++) {// Теперь восстанавливаем состояние каждого флажка в ArrayList, содержащий объекты
                JCheckBox check = (JCheckBox) checkboxList.get(i);//JCheckBox(checkboxList).
                if (checkboxState[i]) {
                    check.setSelected(true);
                } else {
                    check.setSelected(false);
                }
            }
            sequencer.stop();// Здесь мы останавливаем проигрывание мелодии и восстанавливаем последовательность, используя
            buildTrackAndStart();//новые состояния флажков в ArrayList.
        }
    }

}
