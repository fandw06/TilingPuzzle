package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import util.DataFileParser;
import util.Tile;
import dlx.DLX;

/**
 * A class to display the result in a GUI. The DisplayResults instance has two
 * panels, a control panel and a tiles display panel. The control panel includes
 * two parts, configuration and result display. Previously Control panel is
 * programmed in a stand-alone class.
 *
 * @author Dawei Fan, Deyuan Guo
 * @version 1.0 11/16/2014
 *
 *          1.1 11/27/2014 1, Used new class DLX to replace DancingLinks. 2,
 *          Initialized board outside DLX.
 */
public class DisplayDLX extends JPanel implements ActionListener {

	private static final long serialVersionUID = 1L;

	/**
	 * DLX instance. It is initialized after reading a new file.
	 *
	 */
	private DLX dlx;

	/**
	 * The solution will receive from DLX.solve. For every tile, the first
	 * element is the index of the tile, others are positions.
	 *
	 */
	private List<List<List<Integer>>> solution;

	/**
	 * The number of possible solutions.
	 */
	private int numOfSolution;

	/**
	 * The board.
	 */
	private char board[][];

	/** The color of each tile. */
	private List<Color> colors = null;

	/**
	 * Use to map solution from DancingLinks class to actual position. In order
	 * to reduce the time complexity to O(1), an array is used.
	 */
	private int posMap[];

	/**
	 * The speed of the single step and single solution. from 1ms to 500ms.
	 */
	private int speed = 1000;

	/**
	 * Control panel.
	 */
	private JPanel pControl;

	/**
	 * Display panel to show tiles.
	 */
	private JPanel pDisplay;

	/**
	 * TileList Panel
	 */
	private JPanel pTileList;

	/**
	 * Configure panel
	 */
	JPanel pConfig;
	JCheckBox cbEnableSpin;
	JCheckBox cbEnableSpinFlip;
	JCheckBox cbExtra;
	JCheckBox cbRmSymm;
	JButton bSolveAll;
	JButton bSolveStep;
	JButton bSolveTrail;
	JButton bPause;
	JButton bStop;
	JLabel lSpeed;
	JSlider sSpeed;

	/**
	 * Result panel.
	 */
	JPanel pResult;
	JLabel tResultInfo;
	JTextField tIndex;
	JButton bShowResult;
	JButton bPre;
	JButton bNext;
	JButton bPlay;
	JSlider sNumSolution;

	/**
	 * Menu related components.
	 *
	 */
	private static JMenuBar mBar;
	private JMenu mFile;
	private JMenu mHelp;
	private JMenuItem miExit;
	private JMenuItem miRead;
	private JMenuItem miAbout;

	/**
	 * File chooser to get input ASCII files.
	 */
	private JFileChooser fc;

	/**
	 * For auto-playing all solutions.
	 */
	private boolean isRunning = false;

	/**
	 * For auto-playing all solutions. If there is no thread currently, create a
	 * new one.
	 */
	private boolean isThread = false;

	/**
	 * For single step and single solution.
	 */
	private boolean isPaused = false;

	/**
	 * Size parameters.
	 */
	private static final int frameSize[] = { 955, 600 };
	private static final int framePos[] = { 200, 20 };
	private static final int displaySize[] = { 535, 535 };
	private static final int displayPos[] = { 195, 10 };
	private static final int tileListSize[] = { 210, 535 };
	private static final int tileListPos[] = { 735, 10 };

	/**
	 * The origin point from the left-top of the board to pDisplay panel.
	 */
	private int origin[] = { 20, 20 };
	private int originTile[] = { 20 + gridWidth / 2, 20 + gridWidth / 2 };

	private int OffsetX = 5;
	private int OffsetY = 15;

	private static final int gridWidth = 3;

	private int sizeBlock;
	private int sizeTile;

	/**
	 * SwingWorker task for calculating all solutions.
	 */
	private CalculateAll calculateAll = null;

	/**
	 * SwingWorker task for calculating next single solution.
	 */
	private CalculateSingleSolution calculateSSol = null;

	/**
	 * SwingWorker task for calculating next single step.
	 */
	private CalculateSingleStep calculateSStep = null;

	private class CalculateAll extends
			SwingWorker<Void, Void> {

		@Override
		protected Void doInBackground() {

			solution = new ArrayList<List<List<Integer>>>();

			/* Disable all useless buttons to prevent incorrect operations. */
			setControlPanelComponents(false);
			setResultPanelComponents(false);
			bStop.setEnabled(true);
			tIndex.setText("0");
			cleanTiles();

			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			/*
			 * Before every new process must reset DLX (DLX.config is also
			 * reset, DON'T reset enable options)
			 */
			dlx.Config.reset();
			dlx.preProcess();
			dlx.resetSearch();
			cbExtra.setSelected(dlx.Config.isEnableExtra());
			tResultInfo.setText("Searching...");

			List<List<Integer>> sol = dlx.nextSolution();
			while (sol != null && !isCancelled()) {
				solution.add(sol);
				sol = dlx.nextSolution();
			}
			return null;
		}

		@Override
		protected void done() {
			setControlPanelComponents(true);
			setResultPanelComponents(true);
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			/*
			try {
				get();
			} catch (InterruptedException e) {
				System.err.println("The background task has been interrupt!");
			} catch (ExecutionException e) {
				System.err
						.println("The background task has been excuted incorrectly!");
				e.printStackTrace();
			} catch (CancellationException e) {
				System.err.println("The background task has been canceled!");
			}
			*/
			numOfSolution = solution.size();

			/*
			 * After getting the numofSolution, set the min and max of the
			 * slider.
			 */
			/*
			 * Note: only when there is state change the solution could be
			 * displayed!
			 */
			sNumSolution.setMinimum(0);
			sNumSolution.setMaximum(numOfSolution);
			sNumSolution.setValue(0);

			if (numOfSolution == 0)
				tResultInfo.setText("No Solution!");
			else {
				if (numOfSolution == 1)
					tResultInfo.setText("Only 1 Solution!");
				else
					tResultInfo.setText(numOfSolution + " Solutions");
				sNumSolution.setValue(1);
				sNumSolution.setMinimum(1);
			}

			bSolveAll.requestFocus();
		}

	}

	private class CalculateSingleSolution extends
			SwingWorker<Void, Void> {

		@Override
		protected Void doInBackground() {
			solution = new ArrayList<List<List<Integer>>>();

			/* Disable all useless buttons to prevent incorrect operations. */
			setControlPanelComponents(false);
			setResultPanelComponents(false);
			lSpeed.setEnabled(true);
			bPause.setEnabled(true);
			bStop.setEnabled(true);
			sSpeed.setEnabled(true);
			cleanTiles();

			/*
			 * Before every new process must reset DLX (DLX.config is also
			 * reset, DON'T reset enable options)
			 */
			dlx.Config.reset();
			dlx.preProcess();
			dlx.resetSearch();
			cbExtra.setSelected(dlx.Config.isEnableExtra());
			tResultInfo.setText("Searching...");
			tIndex.setText("0");

			List<List<Integer>> sol = dlx.nextSolution();
			while (sol != null && !isCancelled()) {
				try {
					Thread.sleep(speed);
				} catch (InterruptedException e) {
					System.err.println("Sleep interrupt");
				}
				while (isPaused) {
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						System.err.println("Sleep Interrupt!");
					}
				}
				solution.add(sol);
				publish();
				sol = dlx.nextSolution();
			}
			return null;
		}

		@Override
		protected void done() {
			setControlPanelComponents(true);
			setResultPanelComponents(true);

			numOfSolution = solution.size();
			/*
			 * After getting the numofSolution, set the min and max of the
			 * slider.
			 */
			/*
			 * Note: only when there is state change the solution could be
			 * displayed!
			 */
			sNumSolution.setMinimum(0);
			sNumSolution.setMaximum(numOfSolution);
			sNumSolution.setValue(0);

			if (numOfSolution == 0)
				tResultInfo.setText("No Solution!");
			else {
				if (numOfSolution == 1)
					tResultInfo.setText("Only 1 Solution!");
				else
					tResultInfo.setText(numOfSolution + " Solutions");
				sNumSolution.setValue(1);
				sNumSolution.setMinimum(1);
			}

			bSolveStep.requestFocus();
		}

		@Override
		protected void process(List<Void> p) {
			cleanTiles();
			displayStep(solution.get(solution.size() - 1));
			if (!isCancelled())
				tResultInfo.setText("Searching..." + solution.size() + " Solutions");
		}

	}

	private class CalculateSingleStep extends
			SwingWorker<Void, List<List<Integer>>> {

		@Override
		protected Void doInBackground() {
			solution = new ArrayList<List<List<Integer>>>();

			setControlPanelComponents(false);
			setResultPanelComponents(false);
			lSpeed.setEnabled(true);
			bPause.setEnabled(true);
			bStop.setEnabled(true);
			sSpeed.setEnabled(true);
			cleanTiles();

			/*
			 * Before every new process must reset DLX (DLX.config is also
			 * reset, DON'T reset enable options)
			 */
			dlx.Config.reset();
			dlx.preProcess();
			dlx.resetSearch();
			cbExtra.setSelected(dlx.Config.isEnableExtra());

			tResultInfo.setText("Searching...");
			tIndex.setText("0");

			List<List<Integer>> sol = dlx.nextSingleStep();
			while (sol != null && !isCancelled()) {
				try {
					Thread.sleep(speed);
				} catch (InterruptedException e) {
					System.err.println("Sleep Interrupt!");
				}
				while (isPaused) {
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						System.err.println("Sleep Interrupt!");
					}
				}
				publish(sol);
				sol = dlx.nextSingleStep();
				if (dlx.isCompleteSolution()) solution.add(sol);
			}
			return null;
		}

		@Override
		protected void done() {

			setControlPanelComponents(true);
			setResultPanelComponents(true);

			try {
				get();
			} catch (InterruptedException e) {
				System.err.println("The background task has been interrupted!");
			} catch (ExecutionException e) {
				System.err
						.println("The background task has been exccuted incorrectly!");
			} catch (CancellationException e) {
				System.err.println("The background task has been canceled!");
			}
			numOfSolution = solution.size();
			/*
			 * After getting the numofSolution, set the min and max of the
			 * slider.
			 */
			/*
			 * Note: only when there is state change the solution could be
			 * displayed!
			 */
			sNumSolution.setMinimum(0);
			sNumSolution.setMaximum(numOfSolution);
			sNumSolution.setValue(0);

			if (numOfSolution == 0)
				tResultInfo.setText("No Solution!");
			else {
				if (numOfSolution == 1)
					tResultInfo.setText("Only 1 Solution!");
				else
					tResultInfo.setText(numOfSolution + " Solutions");
				sNumSolution.setValue(1);
				sNumSolution.setMinimum(1);
			}

			bSolveTrail.requestFocus();
		}

		@Override
		protected void process(List<List<List<Integer>>> r) {
			cleanTiles();
			displayStep(r.get(r.size() - 1));
			if (!isCancelled())
				tResultInfo.setText("Searching..." + solution.size() + " Solutions");
		}

	}

	public DisplayDLX() {
		super(null);
		setBackground(Color.WHITE);

		this.setLocation(0, 0);
		this.setSize(810, 520);
		this.setOpaque(true);
		this.setVisible(true);
		this.setFocusable(true);

		setupMenu();
		setupControlPanel();
		setupDisplayPanel();
		setupTileListPanel();

		setControlPanelComponents(false);
		setResultPanelComponents(false);
	}

	private void setupControlPanel() {

		pControl = new JPanel();
		pControl.setBackground(Color.WHITE);
		pControl.setLocation(5, 5);
		pControl.setSize(190, 540);
		pControl.setOpaque(true);
		pControl.setVisible(true);
		pControl.setFocusable(true);
		pControl.setLayout(null);
		/*
		 * this.setBorder(BorderFactory.createCompoundBorder(
		 * BorderFactory.createTitledBorder(""),
		 * BorderFactory.createEmptyBorder(5,5,5,5)));
		 */

		/* Initialize control sub-panel. */
		pConfig = new JPanel();
		pConfig.setBackground(Color.WHITE);
		pConfig.setLocation(5, 5);
		pConfig.setSize(180, 350);
		pConfig.setOpaque(true);
		pConfig.setVisible(true);
		pConfig.setFocusable(true);
		pConfig.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("Control"),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)));

		pConfig.setLayout(null);

		cbEnableSpin = new JCheckBox("Enable Rotation");
		cbEnableSpin.setBackground(Color.WHITE);
		cbEnableSpin.setFont(new Font("Lucida Grande", Font.PLAIN, 12));
		cbEnableSpin.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		cbEnableSpin.setSelected(false);
		cbEnableSpin.setSize(160, 30);
		cbEnableSpin.setLocation(10, 20);
		cbEnableSpin.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				if (cbEnableSpin.isSelected())
					dlx.Config.setEnableSpin(true);
				else {
					dlx.Config.setEnableSpin(false);
					cbEnableSpinFlip.setSelected(false);
					dlx.Config.setEnableSpinFlip(false);
				}
			}

		});
		pConfig.add(cbEnableSpin);

		cbEnableSpinFlip = new JCheckBox("Rotation + Reflection");
		cbEnableSpinFlip.setBackground(Color.WHITE);
		cbEnableSpinFlip.setFont(new Font("Lucida Grande", Font.PLAIN, 12));
		cbEnableSpinFlip.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		cbEnableSpinFlip.setSelected(false);
		cbEnableSpinFlip.setSize(160, 30);
		cbEnableSpinFlip.setLocation(10, 50);
		cbEnableSpinFlip.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				if (cbEnableSpinFlip.isSelected()) {
					dlx.Config.setEnableSpinFlip(true);
					cbEnableSpin.setSelected(true);
					dlx.Config.setEnableSpin(true);
				} else
					dlx.Config.setEnableSpinFlip(false);
			}

		});
		pConfig.add(cbEnableSpinFlip);

		cbExtra = new JCheckBox("Extra Blocks");
		cbExtra.setBackground(Color.WHITE);
		cbExtra.setFont(new Font("Lucida Grande", Font.PLAIN, 12));
		cbExtra.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		cbExtra.setSelected(false);
		cbExtra.setSize(160, 30);
		cbExtra.setLocation(10, 80);
		cbExtra.setEnabled(false);
		pConfig.add(cbExtra);

		cbRmSymm = new JCheckBox("Remove Symmetry");
		cbRmSymm.setBackground(Color.WHITE);
		cbRmSymm.setFont(new Font("Lucida Grande", Font.PLAIN, 12));
		cbRmSymm.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		cbRmSymm.setSelected(true);
		cbRmSymm.setSize(160, 30);
		cbRmSymm.setLocation(10, 110);
		cbRmSymm.setEnabled(true);
		cbRmSymm.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (cbRmSymm.isSelected())
					dlx.Config.setEliminateSymmetry(true);
				else
					dlx.Config.setEliminateSymmetry(false);
			}

		});
		pConfig.add(cbRmSymm);

		bSolveAll = new JButton("Get All Solutions");
		bSolveAll.setBackground(Color.WHITE);
		bSolveAll.setFont(new Font("Lucida Grande", Font.PLAIN, 12));
		bSolveAll.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				calculateAll = new CalculateAll();
				calculateAll.execute();
			}

		});
		bSolveAll.setSize(160, 30);
		bSolveAll.setLocation(10, 150);
		pConfig.add(bSolveAll);

		bSolveStep = new JButton("Display Solutions!");
		bSolveStep.setBackground(Color.WHITE);
		bSolveStep.setFont(new Font("Lucida Grande", Font.PLAIN, 12));
		bSolveStep.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				calculateSSol = new CalculateSingleSolution();
				calculateSSol.execute();
			}

		});
		bSolveStep.setSize(160, 30);
		bSolveStep.setLocation(10, 190);
		pConfig.add(bSolveStep);

		bSolveTrail = new JButton("Display Single Steps!");
		bSolveTrail.setBackground(Color.WHITE);
		bSolveTrail.setFont(new Font("Lucida Grande", Font.PLAIN, 12));
		bSolveTrail.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				calculateSStep = new CalculateSingleStep();
				calculateSStep.execute();
			}

		});
		bSolveTrail.setSize(160, 30);
		bSolveTrail.setLocation(10, 230);
		pConfig.add(bSolveTrail);

		lSpeed = new JLabel("Speed (1x)");
		lSpeed.setSize(140, 20);
		lSpeed.setLocation(10, 265);
		pConfig.add(lSpeed);

		sSpeed = new JSlider(-2, 6, 0); // from 2^-2 to 2^6
		sSpeed.setBackground(Color.WHITE);
		sSpeed.setSize(160, 30);
		sSpeed.setLocation(10, 280);
		sSpeed.setSnapToTicks(false);
		sSpeed.setPaintTicks(false);
		sSpeed.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent arg0) {
				int s = sSpeed.getValue();
				if (s < 6) {
					speed = (int) (1000 / Math.pow(2, sSpeed.getValue()));
				} else {
					speed = 1; // unlimited
				}
				if (s >= 0 && s < 6) {
					lSpeed.setText("Speed (" + (int) Math.pow(2, s) + "x)");
				} else if (s < 0) {
					lSpeed.setText("Speed (1/" + (int) Math.pow(2, -s) + "x)");
				} else {
					lSpeed.setText("Speed (Unlimited)");
				}
			}

		});
		pConfig.add(sSpeed);

		bPause = new JButton("Pause");

		bPause.setBackground(Color.WHITE);
		bPause.setFont(new Font("Lucida Grande", Font.PLAIN, 12));
		bPause.setEnabled(false);
		bPause.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				/*
				 * Only if there is at least on thread running, does this button
				 * work.
				 */
				if ((calculateSSol != null && calculateSSol.getState() == SwingWorker.StateValue.STARTED)
						|| (calculateSStep != null && calculateSStep.getState() == SwingWorker.StateValue.STARTED)
						|| (calculateAll != null && calculateAll.getState() == SwingWorker.StateValue.STARTED)) {
					isPaused = !isPaused;
					if (isPaused)
						bPause.setText("Resume");
					else
						bPause.setText("Pause");
				}
			}

		});
		bPause.setSize(75, 30);
		bPause.setLocation(10, 310);
		pConfig.add(bPause);

		bStop = new JButton("Stop");
		bStop.setBackground(Color.WHITE);
		bStop.setFont(new Font("Lucida Grande", Font.PLAIN, 12));
		bStop.setEnabled(false);
		bStop.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				/* Reset the resume button to "Pause" again. */
				bPause.setText("Pause");
				isPaused = false;
				if (calculateSSol != null
						&& calculateSSol.getState() == SwingWorker.StateValue.STARTED) {
					calculateSSol.cancel(true);
					tResultInfo.setText(solution.size()+" Solutions (Cancelled)");
				} else if (calculateSStep != null
						&& calculateSStep.getState() == SwingWorker.StateValue.STARTED) {
					calculateSStep.cancel(true);
					tResultInfo.setText(solution.size()+" Solutions (Cancelled)");
				} else if (calculateAll != null
						&& calculateAll.getState() == SwingWorker.StateValue.STARTED) {
					calculateAll.cancel(true);
					tResultInfo.setText(solution.size()+" Solutions (Cancelled)");
				}
			}

		});
		bStop.setSize(75, 30);
		bStop.setLocation(95, 310);
		pConfig.add(bStop);

		pControl.add(pConfig);

		/* Initialize result sub-panel. */
		pResult = new JPanel();
		pResult.setBackground(Color.WHITE);
		pResult.setSize(180, 180);
		pResult.setLocation(5, 360);
		pResult.setOpaque(true);
		pResult.setVisible(true);
		pResult.setFocusable(true);
		pResult.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("Result"),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)));
		pResult.setLayout(null);

		tResultInfo = new JLabel("Please Select a Puzzle File");
		tResultInfo.setBackground(Color.WHITE);
		tResultInfo.setFont(new Font("Lucida Grande", Font.PLAIN, 12));
		// tResultInfo.setPreferredSize(new Dimension(60, 30));
		tResultInfo.setSize(new Dimension(160, 30));
		tResultInfo.setLocation(10, 20);
		tResultInfo.setVisible(true);
		pResult.add(tResultInfo);

		tIndex = new JTextField("Index", 10);
		tIndex.setBackground(Color.WHITE);
		tIndex.setFont(new Font("Lucida Grande", Font.PLAIN, 12));
		// tIndex.setPreferredSize(new Dimension(80, 25));
		tIndex.setSize(new Dimension(80, 30));
		// tIndex.setMaximumSize(new Dimension(80, 25));
		tIndex.setVisible(true);
		tIndex.setLocation(10, 50);
		tIndex.setHorizontalAlignment(SwingConstants.CENTER);
		pResult.add(tIndex);

		bShowResult = new JButton("Show");
		bShowResult.setBackground(Color.WHITE);
		bShowResult.setFont(new Font("Lucida Grande", Font.PLAIN, 12));
		bShowResult.setSize(80, 30);
		bShowResult.setLocation(90, 50);
		bShowResult.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				int id = 0;
				try {
					id = Integer.parseInt(tIndex.getText());
					if (numOfSolution > 0) {
						if (id < 1) id = 1;
						else if (id > numOfSolution) id = numOfSolution;
					} else {
						id = 0;
					}
				} catch (NumberFormatException e) {
					if (numOfSolution > 0) {
						id = 1;
					} else {
						id = 0;
					}
				}
				cleanTiles();
				if (id > 0) displayResults(id - 1);
				tIndex.setText(Integer.toString(id));
				sNumSolution.setValue(id);
			}

		});
		pResult.add(bShowResult);

		bPre = new JButton("Prev");
		bPre.setBackground(Color.WHITE);
		bPre.setFont(new Font("Lucida Grande", Font.PLAIN, 12));
		bPre.setSize(80, 30);
		bPre.setLocation(10, 80);
		bPre.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				int id = 0;
				try {
					id = Integer.parseInt(tIndex.getText());
					if (numOfSolution > 0) {
						if (id < 1) id = 1;
						else if (id > numOfSolution) id = numOfSolution;
						else if (id > 1) id--;
					} else {
						id = 0;
					}
				} catch (NumberFormatException e) {
					if (numOfSolution > 0) {
						id = 1;
					} else {
						id = 0;
					}
				}
				cleanTiles();
				if (id > 0) displayResults(id - 1);
				tIndex.setText(Integer.toString(id));
				sNumSolution.setValue(id);
			}

		});
		pResult.add(bPre);

		bNext = new JButton("Next");
		bNext.setBackground(Color.WHITE);
		bNext.setFont(new Font("Lucida Grande", Font.PLAIN, 12));
		bNext.setSize(80, 30);
		bNext.setLocation(90, 80);
		bNext.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				int id = 0;
				try {
					id = Integer.parseInt(tIndex.getText());
					if (numOfSolution > 0) {
						if (id < 1) id = 1;
						else if (id > numOfSolution) id = numOfSolution;
						else if (id < numOfSolution) id++;
					} else {
						id = 0;
					}
				} catch (NumberFormatException e) {
					if (numOfSolution > 0) {
						id = 1;
					} else {
						id = 0;
					}
				}
				cleanTiles();
				if (id > 0) displayResults(id - 1);
				tIndex.setText(Integer.toString(id));
				sNumSolution.setValue(id);
			}

		});
		pResult.add(bNext);

		sNumSolution = new JSlider();
		sNumSolution.setBackground(Color.WHITE);
		sNumSolution.setSize(160, 30);
		sNumSolution.setLocation(10, 110);
		sNumSolution.setExtent(0);
		sNumSolution.setMinimum(0);
		sNumSolution.setMaximum(0);
		sNumSolution.setValue(0);
		sNumSolution.setSnapToTicks(false);
		sNumSolution.setPaintTicks(false);
		sNumSolution.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent arg0) {
				int id = sNumSolution.getValue();
				tIndex.setText(Integer.toString(id));
				cleanTiles();
				if (id >= 1 && id <= numOfSolution)
					displayResults(id - 1);
			}

		});

		pResult.add(sNumSolution);

		bPlay = new JButton("Autoplay All Solutions");
		bPlay.setBackground(Color.WHITE);
		bPlay.setFont(new Font("Lucida Grande", Font.PLAIN, 12));
		bPlay.setSize(160, 30);
		bPlay.setLocation(10, 140);
		bPlay.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (!isRunning && numOfSolution > 0) {
					isRunning = true;
					bPlay.setText("Stop");
					setControlPanelComponents(false);
					tIndex.setEnabled(false);
					bShowResult.setEnabled(false);
					bPre.setEnabled(false);
					bNext.setEnabled(false);
					lSpeed.setEnabled(true);
					sSpeed.setEnabled(true);

					if (!isThread) {
						isThread = true;
						new Thread(new Runnable() {

							@Override
							public void run() {
								// Get current value in tIndex
								int id = 0;
								try {
									id = Integer.parseInt(tIndex.getText());
									if (numOfSolution > 0) {
										if (id < 1) id = 1;
										else if (id > numOfSolution) id = numOfSolution;
									} else {
										id = 0;
									}
								} catch (NumberFormatException e) {
									if (numOfSolution > 0) {
										id = 1;
									} else {
										id = 0;
									}
								}
								if (id == numOfSolution) id = 1;
								// Loop show
								while (id > 0 && id <= numOfSolution && isRunning) {
									cleanTiles();
									displayResults(id - 1);
									tIndex.setText(Integer.toString(id));
									sNumSolution.setValue(id);
									try {
										int s = speed;
										if (s < 200) {
											Thread.sleep(speed);
										} else {
											while (s > 0) {
												Thread.sleep(200);
												s -= 200;
												if (!isRunning) break;
											}
										}
									} catch (InterruptedException e) {
										System.err.println("Sleep interrupt");
									}
									id++;
								}
								// Finished
								isThread = false;
								isRunning = false;
								bPlay.setText("Autoplay All Solutions");
								setControlPanelComponents(true);
								tIndex.setEnabled(true);
								bShowResult.setEnabled(true);
								bPre.setEnabled(true);
								bNext.setEnabled(true);
							}
						}).start();

					}
				}

				/* If now it is running, stop it! */
				else {
					isRunning = false;
					bPlay.setText("Autoplay All Solutions");
				}
			}
		});
		pResult.add(bPlay);
		pControl.add(pResult);
		this.add(pControl);
	}

	private void setupDisplayPanel() {
		pDisplay = new JPanel();
		pDisplay.setBackground(Color.WHITE);
		pDisplay.setLayout(null);
		pDisplay.setLocation(displayPos[0], displayPos[1]);
		pDisplay.setSize(displaySize[0], displaySize[1]);
		pDisplay.setOpaque(true);
		pDisplay.setVisible(true);
		pDisplay.setFocusable(true);
		pDisplay.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("Solution"),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)));

		this.add(pDisplay);
	}

	private void setupTileListPanel() {
		pTileList = new JPanel();
		pTileList.setBackground(Color.WHITE);
		pTileList.setLayout(null);
		pTileList.setSize(tileListSize[0], tileListSize[1]);
		pTileList.setLocation(tileListPos[0], tileListPos[1]);
		pTileList.setOpaque(true);
		pTileList.setVisible(true);
		pTileList.setFocusable(true);
		pTileList.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("Tiles"),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)));

		this.add(pTileList);
	}

	private void setupTileList(List<Tile> tiles, Tile board) {

		// for (int i = 0; i < tiles.size(); i++) {
		// tiles.get(i).printTile();
		// }

		boolean show_char = false;
		char c = ' ';
		for (int i = 0; i < board.data.length && !show_char; i++) {
			for (int j = 0; j < board.data[0].length && !show_char; j++) {
				if (board.data[i][j] != ' ') {
					if (c == ' ') {
						c = board.data[i][j];
					} else {
						if (board.data[i][j] != c)
							show_char = true;
					}
				}
			}
		}

		final int tileListAreaWidth = tileListSize[0] - 30;
		final int tileListAreaHeight = tileListSize[1] - 45;

		JPanel pic = new JPanel(null);
		pic.setSize(tileListAreaWidth, tileListAreaHeight);
		pic.setLocation(15, 30);
		pic.setBackground(Color.white);
		pTileList.add(pic);

		int[][] pack = Tile.packAllTiles(tiles, (double) tileListAreaHeight
				/ tileListAreaWidth);

		int grid = Math.min((tileListAreaWidth - 1) / pack[0].length,
				(tileListAreaHeight - 1) / pack.length);
		int[][] tbase = new int[tiles.size()][2];
		for (int i = 0; i < tiles.size(); i++)
			tbase[i][0] = -1;
		for (int i = 0; i < pack.length; i++) {
			for (int j = 0; j < pack[0].length; j++) {
				int id = pack[i][j];
				if (id >= 0) {
					if (tbase[id][0] < 0) {
						tbase[id][0] = i;
						tbase[id][1] = j;
					}
					int ofsty = i - tbase[id][0];
					int ofstx = j - tbase[id][1];
					char t = tiles.get(id).data[ofsty][ofstx];
					if (t != ' ') {
						JPanel block = new JPanel();
						block.setBackground(colors.get(pack[i][j]));
						block.setBorder(new LineBorder(Color.black));
						block.setSize(grid + 1, grid + 1);
						block.setLocation(j * grid, i * grid);
						if (show_char && grid > 5) {
							JLabel l = new JLabel(Character.toString(t));
							l.setSize(grid - 2, grid - 2);
							l.setFont(new Font("Arial", Font.PLAIN, grid - 2));
							l.setVerticalAlignment(SwingConstants.CENTER);
							l.setHorizontalAlignment(SwingConstants.CENTER);
							l.setLocation(j * grid + 1, i * grid + 1);
							l.setOpaque(false); // false = transparent
							l.setVisible(true);
							pic.add(l);
						}
						pic.add(block);
					}
				}
			}
		}
	}

	private void setupMenu() {
		mBar = new JMenuBar();
		mBar.setBackground(Color.WHITE);
		mBar.setOpaque(true);

		mFile = new JMenu("File");
		mFile.setMnemonic(KeyEvent.VK_F);

		miRead = new JMenuItem("Read...");
		miRead.setMnemonic(KeyEvent.VK_R);
		miRead.addActionListener(this);
		mFile.add(miRead);

		miExit = new JMenuItem("Exit");
		miExit.setMnemonic(KeyEvent.VK_E);
		miExit.addActionListener(this);
		mFile.add(miExit);

		mHelp = new JMenu("Help");
		mHelp.setMnemonic(KeyEvent.VK_H);
		miAbout = new JMenuItem("About");
		miAbout.setMnemonic(KeyEvent.VK_A);
		miAbout.addActionListener(this);
		mHelp.add(miAbout);

		mBar.add(mFile);
		mBar.add(mHelp);

		fc = new JFileChooser();
		fc.setCurrentDirectory(new File("."));

	}

	/**
	 * Initiate the board from input and draw the board.
	 */
	private void setupBoard(char[][] b) {

		/* Initialize board array. */
		board = new char[b.length][b[0].length];
		for (int i = 0; i < b.length; i++) {
			for (int j = 0; j < b[0].length; j++) {
				board[i][j] = b[i][j];
			}
		}

		/*
		 * Initialize board related size. There should be some redundancy for
		 * the sizeblock, or the edges cannot be drawn.
		 */
		sizeBlock = Math.min((displaySize[0] - origin[0] - 20)
				/ board[0].length, (displaySize[1] - origin[1] - 20)
				/ board.length)
				- gridWidth;
		sizeTile = sizeBlock + gridWidth;

		/* There are n kind of colors in the board. */
		Set<Character> set = new HashSet<Character>();

		int w = board.length;
		int l = board[0].length;

		/* Set offset, to make the board display in the middle. */
		OffsetX = displaySize[0] / 2
				- (l * (sizeBlock + gridWidth) + gridWidth) / 2 - 18;
		OffsetY = displaySize[1] / 2
				- (w * (sizeBlock + gridWidth) + gridWidth) / 2 - 10;

		/* First is x, second is y. */
		int sizeGridH[] = { l * (sizeBlock + gridWidth) + gridWidth, gridWidth };
		int sizeGridV[] = { gridWidth, w * (sizeBlock + gridWidth) + gridWidth };
		JPanel gridH[] = new JPanel[w + 1];
		JPanel gridV[] = new JPanel[l + 1];
		JPanel gridH_w[] = new JPanel[w + 1];
		JPanel gridV_w[] = new JPanel[l + 1];

		/* Setup horizontal and vertical white grids */
		for (int i = 0; i <= w; i++) {
			gridH_w[i] = new JPanel();
			gridH_w[i].setBackground(Color.white);
			gridH_w[i].setSize(sizeGridH[0] - 2, sizeGridH[1] - 2);
			gridH_w[i].setLocation(origin[0] + OffsetX, origin[1]
					+ (sizeBlock + gridWidth) * i + OffsetY);
			gridH_w[i].setOpaque(true);
			gridH_w[i].setVisible(true);
			pDisplay.add(gridH_w[i]);
		}

		for (int j = 0; j <= l; j++) {
			gridV_w[j] = new JPanel();
			gridV_w[j].setBackground(Color.white);
			gridV_w[j].setSize(sizeGridV[0] - 2, sizeGridV[1] - 2);
			gridV_w[j].setLocation(origin[0] + (sizeBlock + gridWidth) * j
					+ OffsetX, origin[1] + OffsetY);
			gridV_w[j].setOpaque(true);
			gridV_w[j].setVisible(true);
			pDisplay.add(gridV_w[j]);
		}

		/* Setup horizontal and vertical black grids */
		for (int i = 0; i <= w; i++) {
			gridH[i] = new JPanel();
			gridH[i].setBackground(Color.black);
			gridH[i].setSize(sizeGridH[0], sizeGridH[1]);
			gridH[i].setLocation(origin[0] + OffsetX - 1, origin[1]
					+ (sizeBlock + gridWidth) * i + OffsetY - 1);
			gridH[i].setOpaque(true);
			gridH[i].setVisible(true);
			pDisplay.add(gridH[i]);
		}

		for (int j = 0; j <= l; j++) {
			gridV[j] = new JPanel();
			gridV[j].setBackground(Color.black);
			gridV[j].setSize(sizeGridV[0], sizeGridV[1]);
			gridV[j].setLocation(origin[0] + (sizeBlock + gridWidth) * j
					+ OffsetX - 1, origin[1] + OffsetY - 1);
			gridV[j].setOpaque(true);
			gridV[j].setVisible(true);
			pDisplay.add(gridV[j]);
		}

		/* Setup missing blocks if there are any. */
		for (int i = 0; i < w; i++) {
			for (int j = 0; j < l; j++) {
				if (board[i][j] == ' ') {

					JPanel block = new JPanel() {
						private static final long serialVersionUID = 1L;

						@Override
						public void paintComponent(Graphics g) {
							for (int i = 0; i < sizeTile; i += 4) {
								g.drawLine(sizeTile - i, 0, 0, sizeTile - i);
								g.drawLine(sizeTile, i, i, sizeTile);
							}
						}
					};
					block.setSize(sizeTile, sizeTile);
					int x = originTile[0] + (j) * sizeTile + OffsetX;
					int y = originTile[1] + (i) * sizeTile + OffsetY;
					block.setLocation(x, y);
					block.setOpaque(true);
					block.setVisible(true);
					pDisplay.add(block);
				} else
					set.add(board[i][j]);
			}
		}

		/*
		 * Setup board colors (now use chars to represent) if there are more
		 * than one colors.
		 */
		if (set.size() > 1) {
			for (int i = 0; i < w; i++) {
				for (int j = 0; j < l; j++) {
					if (board[i][j] == ' ')
						continue;
					JLabel block = new JLabel(Character.toString(board[i][j]));
					block.setSize(sizeTile / 2, sizeTile / 2);
					block.setFont(new Font("Arial", Font.PLAIN, sizeTile / 2));
					block.setVerticalAlignment(SwingConstants.CENTER);
					block.setHorizontalAlignment(SwingConstants.CENTER);
					int x = originTile[0] + (j) * sizeTile + OffsetX + sizeTile
							/ 4;
					int y = originTile[1] + (i) * sizeTile + OffsetY + sizeTile
							/ 4;
					block.setLocation(x, y);
					block.setOpaque(false); // set to transparent
					block.setVisible(true);
					pDisplay.add(block);
				}
			}
		}
	}

	public void displayResults(int id) {

		List<List<Integer>> pos = solution.get(id);
		int number = pos.size();

		if (id >= numOfSolution) {
			System.err.println("Index of solutions out of range!");
			return;
		}

		for (int i = 0; i < number; i++) {
			List<Integer> tilePos = new ArrayList<Integer>();
			tilePos = pos.get(i);
			Color c = colors.get(tilePos.get(0));
			for (int j = 1; j < tilePos.size(); j++) {
				JPanel block = new JPanel();
				block.setBackground(c);
				block.setSize(sizeTile, sizeTile);
				int x = originTile[0]
						+ (posMap[tilePos.get(j)] % (board[0].length))
						* sizeTile + OffsetX;
				int y = originTile[0]
						+ (posMap[tilePos.get(j)] / (board[0].length))
						* sizeTile + OffsetY;
				block.setLocation(x, y);
				block.setOpaque(true);
				block.setVisible(true);
				pDisplay.add(block);

			}
		}
		pDisplay.repaint();

	}

	public void displayStep(List<List<Integer>> pos) {

		int number = pos.size();

		for (int i = 0; i < number; i++) {
			List<Integer> tilePos = new ArrayList<Integer>();
			tilePos = pos.get(i);
			Color c = colors.get(tilePos.get(0));
			for (int j = 1; j < tilePos.size(); j++) {
				JPanel block = new JPanel();
				block.setBackground(c);
				block.setSize(sizeTile, sizeTile);
				int x = originTile[0]
						+ (posMap[tilePos.get(j)] % (board[0].length))
						* sizeTile + OffsetX;
				int y = originTile[0]
						+ (posMap[tilePos.get(j)] / (board[0].length))
						* sizeTile + OffsetY;
				block.setLocation(x, y);
				block.setOpaque(true);
				block.setVisible(true);
				pDisplay.add(block);

			}
		}
		pDisplay.repaint();
	}

	private void cleanTiles() {

		for (int i = 0; i < board.length; i++) {
			for (int j = 0; j < board[0].length; j++) {
				/* Avoid deleting the grids. */
				int x = originTile[0] + j * sizeTile + gridWidth + 2 + OffsetX;
				int y = originTile[0] + i * sizeTile + gridWidth + 2 + OffsetY;

				if (board[i][j] != ' ') {
					Component t = pDisplay.getComponentAt(x, y);
					pDisplay.remove(t);
				}
			}
		}
		pDisplay.repaint();
	}

	/**
	 * This is a map between the assigned number of the board in DLX and the
	 * real position for drawing when there are holes in the board. If there are
	 * no holes this function does nothing
	 */
	private void setPosMap() {
		posMap = new int[board.length * board[0].length];
		for (int j = 0; j < posMap.length; j++)
			posMap[j] = j;

		/* This list is for board with missing blocks. */
		List<Integer> missing = new ArrayList<Integer>();
		for (int i = 0; i < board.length; i++) {
			for (int j = 0; j < board[0].length; j++) {
				if (board[i][j] == ' ')
					missing.add(i * board[0].length + j);
			}
		}

		for (int i = 0; i < missing.size(); i++) {
			for (int j = missing.get(i) - i; j < posMap.length; j++)
				posMap[j]++;
		}
	}

	/**
	 * Generate n colors for n different tiles.
	 *
	 * @param n
	 *            - the number of tiles
	 * @return a list of Color
	 */
	private List<Color> genColors(int n) {
		List<Color> colors = new ArrayList<Color>();

		/* Default color list. */
		colors.addAll(Arrays.asList(Color.red, Color.green, Color.blue,
				Color.yellow, Color.cyan, new Color(46, 139, 87), new Color(148,
						0, 211), new Color(135, 51, 36), Color.magenta,
				Color.gray, Color.pink, new Color(175, 255, 225), new Color(
						130, 175, 190)));

		double goldenRatio = 0.618033988749895;
		double hue = 0.0; // or use random start value between 0 and 1
		double saturation = 0.5;
		double value = 0.95;

		int m = n - colors.size();
		for (int i = 0; i < m; i++) {
			hue = (hue + goldenRatio) % 1.0;

			/* HSV to RGB */
			int h = (int) (hue * 6);
			double f = hue * 6 - h;
			double p = value * (1 - saturation);
			double q = value * (1 - f * saturation);
			double t = value * (1 - (1 - f) * saturation);

			double r, g, b;
			switch (h) {
			case 0:
				r = value;
				g = t;
				b = p;
				break;
			case 1:
				r = q;
				g = value;
				b = p;
				break;
			case 2:
				r = p;
				g = value;
				b = t;
				break;
			case 3:
				r = p;
				g = q;
				b = value;
				break;
			case 4:
				r = t;
				g = p;
				b = value;
				break;
			case 5:
				r = value;
				g = p;
				b = q;
				break;
			default:
				throw new RuntimeException("Error in genColor.");
			}

			colors.add(new Color((int) (r * 256), (int) (g * 256),
					(int) (b * 256)));
		}
		return colors;
	}

	private void setControlPanelComponents(boolean b) {
		cbEnableSpin.setEnabled(b);
		cbEnableSpinFlip.setEnabled(b);
		cbRmSymm.setEnabled(b);
		bSolveAll.setEnabled(b);
		bSolveTrail.setEnabled(b);
		bSolveStep.setEnabled(b);
		lSpeed.setEnabled(b);
		sSpeed.setEnabled(b);
		bPause.setEnabled(b);
		bStop.setEnabled(b);
	}

	private void setResultPanelComponents(boolean b) {
		sNumSolution.setEnabled(b);
		tIndex.setEnabled(b);

		bShowResult.setEnabled(b);
		bPre.setEnabled(b);
		bNext.setEnabled(b);
		bPlay.setEnabled(b);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == miExit)
			System.exit(0);

		if (e.getSource() == miAbout) {
			JOptionPane.showMessageDialog(null, "Tiling Puzzle Solver v1.0\n"
					+ "Date: Dec 1, 2014\n"
					+ "Designed by Dawei Fan and Deyuan Guo", "About",
					JOptionPane.INFORMATION_MESSAGE);
		}
		if (e.getSource() == miRead) {
			/* If there is a thread running, cannot select new files. */
			if ((calculateSSol != null && calculateSSol.getState() == SwingWorker.StateValue.STARTED)
					|| (calculateSStep != null && calculateSStep.getState() == SwingWorker.StateValue.STARTED)
					|| (calculateAll != null && calculateAll.getState() == SwingWorker.StateValue.STARTED)) {
				JOptionPane.showConfirmDialog(null,
						"Please stop searching before selecting a new file.",
						"Warning",
						JOptionPane.CLOSED_OPTION, JOptionPane.WARNING_MESSAGE);
				return;
			} else if (isRunning) { // Autoplay..
				JOptionPane.showConfirmDialog(null,
						"Please stop autoplay before selecting a new file.",
						"Warning",
						JOptionPane.CLOSED_OPTION, JOptionPane.WARNING_MESSAGE);
				return;
			}

			/* Read a ASCII file and solve it. */
			File file = null;
			if (fc.showOpenDialog(DisplayDLX.this) == JFileChooser.APPROVE_OPTION) {
				file = fc.getSelectedFile();
			}
			// not select any files
			else {
				JOptionPane.showConfirmDialog(null, "No file is selected.",
						"Warning", JOptionPane.CLOSED_OPTION,
						JOptionPane.WARNING_MESSAGE);
				return;
			}

			/* Delete all panels in the pDisplay and pTileList if there are. */
			pTileList.removeAll();
			pDisplay.removeAll();

			/* Reset configuration . */
			cbEnableSpin.setSelected(false);
			cbEnableSpinFlip.setSelected(false);
			cbRmSymm.setSelected(true);

			/* Reset text field */
			tResultInfo.setText("Press Buttons to Solve");
			tIndex.setText("0");
			numOfSolution = 0;
			sNumSolution.setMinimum(0);
			sNumSolution.setMaximum(0);
			sNumSolution.setValue(0);

			pDisplay.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createTitledBorder("Solution ("
							+ file.getName() + ")"),
					BorderFactory.createEmptyBorder(5, 5, 5, 5)));
			DataFileParser dfp = new DataFileParser(file.getAbsolutePath());
			/* Extract puzzle pieces, board are included in this list. */
			List<Tile> tileList = dfp.ExtractTiles();
			/* Get the board and the remained is tileList. */
			Tile board = tileList.get(0);
			tileList.remove(0);

			/* Initiate the color list */
			colors = genColors(tileList.size());

			/* Initiate a new DLX Solver and set it. */
			DLX dlx = new DLX(board, tileList);
			this.dlx = dlx;
			/* enable control panel components. */
			setControlPanelComponents(true);
			setResultPanelComponents(true);
			/* Initialize the board and posMap. */
			setupBoard(board.data);
			setPosMap();
			/* Initialize and display the tile. */
			setupTileList(tileList, board);
			repaint();
		}
	}

	public static void createAndShowGUI() {

		JFrame frame = new JFrame("Tilling Puzzle Solver");
		frame.setContentPane(new DisplayDLX());
		frame.setJMenuBar(mBar);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout());
		frame.setSize(frameSize[0], frameSize[1]);
		frame.setLocation(framePos[0], framePos[1]);
		frame.setResizable(false);
		frame.setVisible(true);
	}

	public static void main(String[] args) {

		try {
			UIManager
					.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (Exception e) {
			e.printStackTrace();
		}

		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				createAndShowGUI();
			}

		});

	}
}