import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.function.BiFunction;

import static java.lang.Math.min;

/*
 * Licensed under a public domain‐like license. See Main.java for license text.
 */

public class GUIMain extends Main {
    private static Thread worker = null;

    static private JCheckBox option_enable_filter_dictionary;
    static private JCheckBox option_enable_filter_punctuation;

    static private JCheckBox option_strip_furigana;
    static private JCheckBox option_enable_linecount;
    static private JCheckBox option_enable_userdict;
    static private JCheckBox option_enable_userfilter;
    
    static private JCheckBox option_pull_out_spellings;

    public static void main(String[] args)
    {
        System.out.println("To avoid the GUI, use command line parameters. See --help.");
        SwingUtilities.invokeLater( () ->
        {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException|InstantiationException|IllegalAccessException|UnsupportedLookAndFeelException e) { /* */ }

            Image icon = new ImageIcon(Main.class.getResource("icon.png")).getImage();
            JFrame window = new JFrame();
            window.setIconImage(icon);
            window.setTitle("unnamed japanese text analysis tool");
            window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            window.setResizable(false);

            Container pane = window.getContentPane();
            pane.setLayout(null);
            pane.setSize(400, 300);


            JLabel explanation1 = new JLabel("frequency list generator");

            JLabel explanation2 = new JLabel("Input must be in UTF-8.");
            JButton input = new JButton("Input");
            JButton write = new JButton("Output");
            JTextField field_input = new JTextField("");
            JTextField field_write = new JTextField("");

            JLabel explanation3 = new JLabel("Filters:");
            option_enable_filter_dictionary = new JCheckBox("Require term to be in dictionary", true);
            option_enable_filter_punctuation = new JCheckBox("Disallow punctuation", true);
            option_enable_userfilter = new JCheckBox("Load filters from userfilter.csv", true);

            JLabel explanation4 = new JLabel("Other options:");
            option_strip_furigana = new JCheckBox("Strip 《》 furigana (occurs before parsing) (also deletes 〈 and 〉)", false);
            option_enable_linecount = new JCheckBox("Include index of line of first occurrence", false);
            option_enable_userdict = new JCheckBox("Load additional user dictionary from userdict.csv", true);
            
            option_pull_out_spellings = new JCheckBox("\"Pull out\" spellings", false);

            JButton run = new JButton("Run");
            JProgressBar progress = new JProgressBar();


            input.setMargin(new Insets(5,5,5,5));
            write.setMargin(new Insets(5,5,5,5));
            progress.setStringPainted(true);
            progress.setString("Waiting to be run");

            input.addActionListener((e)->
            {
                FileDialog d = new FileDialog((java.awt.Frame) null, "Corpus (input)", FileDialog.LOAD);
                d.setVisible(true);
                if(d.getFile() != null)
                    field_input.setText(d.getDirectory()+d.getFile());
            });
            write.addActionListener((e)->
            {
                FileDialog d = new java.awt.FileDialog((java.awt.Frame) null, "Frequency list (output)", FileDialog.SAVE);
                d.setVisible(true);
                if(d.getFile() != null)
                    field_write.setText(d.getDirectory()+d.getFile());
            });

            run.addActionListener((a)->
            {
                filter_dictionary_enabled = option_enable_filter_dictionary.isSelected();
                filter_punctuation_enabled = option_enable_filter_punctuation.isSelected();

                skip_furigana_formatting = option_strip_furigana.isSelected();
                enable_linecounter = option_enable_linecount.isSelected();
                
                enable_userdictionary = option_enable_userdict.isSelected();
                enable_userfilter = option_enable_userfilter.isSelected();
                
                pull_out_spellings = option_pull_out_spellings.isSelected();

                if(worker != null && worker.isAlive()) return;
                worker = new Thread(() ->
                {
                    try
                    {
                        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(field_write.getText()), "UTF-8"));
                        run(field_input.getText(), writer, (text, length) ->
                        {
                            progress.setString(text);
                            if(text.equals("Done"))
                            {
                                progress.setIndeterminate(false);
                                progress.setValue(0);
                            }
                            else if(length >= 0.0)
                            {
                                progress.setIndeterminate(false);
                                progress.setMaximum(100000000);
                                progress.setValue((int)(length*100000000));
                            }
                            else
                            {
                                progress.setIndeterminate(true);
                            }
                        });
                        writer.close();
                    }
                    catch (UnsupportedEncodingException e)
                    {
                        progress.setString("Failed to open output as UTF-8.");
                    }
                    catch (FileNotFoundException e)
                    {
                        progress.setString("Failed to open output file.");
                    }
                    catch (IOException e)
                    {
                        progress.setString("Error while closing output file.");
                    }
                });
                worker.start();
            });


            // adds fullwidth elements
            BiFunction<JComponent, Integer, Integer> adder = (element, y) ->
            {
                element.setBounds(5, y, min(pane.getWidth()-10, element.getPreferredSize().width), element.getPreferredSize().height);
                return y + element.getPreferredSize().height;
            };
            Integer row = 5;

            explanation1.setBounds(5, 5, pane.getWidth()-10, 20); explanation1.setHorizontalAlignment(SwingConstants.CENTER); row += 25;

            row = adder.apply(explanation2, row); row += 5;
            input.setBounds(5, row, 65, 20); field_input.setBounds(75, row, pane.getWidth()-75-10, 20); row += 25;
            write.setBounds(5, row, 65, 20); field_write.setBounds(75, row, pane.getWidth()-75-10, 20); row += 25;

            row += 3; row = adder.apply(explanation3, row); row += 3;
            row = adder.apply(option_enable_filter_dictionary, row);
            row = adder.apply(option_enable_filter_punctuation, row);
            row = adder.apply(option_enable_userfilter, row);

            row += 3; row = adder.apply(explanation4, row); row += 3;
            row = adder.apply(option_strip_furigana, row);
            row = adder.apply(option_enable_linecount, row);
            row = adder.apply(option_enable_userdict, row);
            row = adder.apply(option_pull_out_spellings, row);
            row += 5;

            run.setBounds(5, row, 65, 20); progress.setBounds(75, row, pane.getWidth()-75-10, 20); row += 25;


            pane.add(explanation1);
            pane.add(explanation2);
            pane.add(explanation3);
            pane.add(explanation4);

            pane.add(input);
            pane.add(write);
            pane.add(field_input);
            pane.add(field_write);

            pane.add(option_enable_filter_dictionary);
            pane.add(option_enable_filter_punctuation);
            pane.add(option_enable_userfilter);

            pane.add(option_strip_furigana);
            pane.add(option_enable_linecount);
            pane.add(option_enable_userdict);
            pane.add(option_pull_out_spellings);

            pane.add(run);
            pane.add(progress);


            pane.setPreferredSize(new Dimension(400, row));
            window.pack();
            window.setLocationRelativeTo(null);
            window.setVisible(true);
        });
    }
}
