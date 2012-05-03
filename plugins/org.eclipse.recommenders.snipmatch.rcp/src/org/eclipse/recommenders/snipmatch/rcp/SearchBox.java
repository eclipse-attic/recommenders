/**
 * Copyright (c) 2011 Doug Wightman, Zi Ye
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.recommenders.snipmatch.rcp;

import java.util.ArrayList;

import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.recommenders.snipmatch.core.ArgumentMatchNode;
import org.eclipse.recommenders.snipmatch.core.EffectMatchNode;
import org.eclipse.recommenders.snipmatch.core.MatchEnvironment;
import org.eclipse.recommenders.snipmatch.core.MatchNode;
import org.eclipse.recommenders.snipmatch.search.ClientSwitcher;
import org.eclipse.recommenders.snipmatch.web.ISearchListener;
import org.eclipse.recommenders.snipmatch.web.ISendFeedbackListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;

/**
 * This is the main search interface.
 */
public class SearchBox extends ClientSwitcher{

	private final int idealWidth = 360;
	private final int idealHeight = 200;
	private final int maxMatches = 10;
	private Color queryBackColor;
	private Color queryDisabledBackColor;
	private Color matchSelectBackColor;
	private Color matchSelectOverviewBackColor;
	//private Color matchKeywordForeColor;
	//private Color matchArgForeColor;
	//private Color matchBlankArgForeColor;
	private Font searchFont;
	//private Cursor arrowCursor;
	//private Cursor handCursor;

	private MatchEnvironment env;
	private Shell shell;
	private StyledText queryText;
	private ArrayList<MatchNode> matches;
	private ArrayList<CompleteMatchThread> completeMatchThreads;
	private Shell buttonBar;
	//private Shell refineDialog;
	private boolean noResultsYet;
	private int selection;
	private boolean selectionConfirmed;
	private CloseOnIgnoreListener ignoredListener;
	private boolean ignoreTextChange;
	//private EffectMatchNode refinedMatch;
	
	private Shell resultDisplayShell = null;
	private Table resultDisplayTable = null;
	private Shell resultOverviewShell = null;
	private StyledText resultOverviewPanel = null;

	public SearchBox() {
	}
	
	public void show(final String envName) {	
		IThemeManager themeManager = PlatformUI.getWorkbench().getThemeManager();
		ITheme currentTheme = themeManager.getCurrentTheme();
		ColorRegistry colorRegistry = currentTheme.getColorRegistry();
		queryBackColor = colorRegistry.get("org.eclipse.recommenders.snipmatch.preferences.colorDefinition.queryBackColor");
		queryDisabledBackColor = colorRegistry.get("org.eclipse.recommenders.snipmatch.preferences.colorDefinition.queryDisabledBackColor");;
		matchSelectBackColor = colorRegistry.get("org.eclipse.recommenders.snipmatch.preferences.colorDefinition.matchSelectBackColor");
		matchSelectOverviewBackColor = colorRegistry.get("org.eclipse.recommenders.snipmatch.preferences.colorDefinition.selectionOverviewBackColor");
		//matchKeywordForeColor = colorRegistry.get("org.eclipse.recommenders.snipmatch.preferences.colorDefinition.matchKeywordForeColor");
		//matchArgForeColor = colorRegistry.get("org.eclipse.recommenders.snipmatch.preferences.colorDefinition.matchArgForeColor");
		//matchBlankArgForeColor = colorRegistry.get("org.eclipse.recommenders.snipmatch.preferences.colorDefinition.matchBlankArgForeColor");
		
		FontRegistry fontRegistry = currentTheme.getFontRegistry();
		searchFont = fontRegistry.get("org.eclipse.recommenders.snipmatch.preferences.fontDefinition");
		//arrowCursor = new Cursor(PlatformUI.getWorkbench().getDisplay(), SWT.CURSOR_ARROW);
		//handCursor = new Cursor(PlatformUI.getWorkbench().getDisplay(), SWT.CURSOR_HAND);
		

		if (envName.equals("javasnippet")) {

			env = new JavaSnippetMatchEnvironment();

			if (((JavaSnippetMatchEnvironment) env).getProject() == null) {

				MessageBox popup = new MessageBox(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
						SWT.ICON_ERROR | SWT.OK | SWT.APPLICATION_MODAL);

				popup.setText("SnipMatch");
				popup.setMessage("Searching from independent source files is not"
						+ "supported at this time. Please make sure the source file "
						+ "is part of an open Java project.");
				popup.open();

				return;
			}
		}

		if (shell == null || shell.isDisposed()) {

			shell = new Shell(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.NO_TRIM
					| SWT.NO_FOCUS | SWT.NO_BACKGROUND);

			shell.setBackgroundMode(SWT.INHERIT_DEFAULT);

			{
				queryText = new StyledText(shell, SWT.BORDER);
				queryText.setBackground(queryBackColor);
				queryText.setMargins(8, 6, 8, 6);
				queryText.setFont(searchFont);
				queryText.setSize(idealWidth, queryText.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
				queryText.addKeyListener(resultDisplayKeyListener);
				
				queryText.addModifyListener(new ModifyListener() {

					@Override
					public void modifyText(ModifyEvent e) {
						handleTyping();
					}
				});

				queryText.addKeyListener(new KeyListener() {

					@Override
					public void keyReleased(KeyEvent e) {
					}

					@Override
					public void keyPressed(KeyEvent e) {

						switch (e.keyCode) {

						case '\t':

							// If tab is pressed, then tab-complete the search
							// query to the next argument.

							if (selection != -1) {

								EffectMatchNode match = (EffectMatchNode) matches.get(selection);

								ArrayList<int[]> argRanges = new ArrayList<int[]>();
								ArrayList<int[]> blankArgRanges = new ArrayList<int[]>();

								String matchString = "";
								try{
									matchString = buildMatchString(match, argRanges, blankArgRanges, false, 0);
								}catch(Exception matche){
									matche.printStackTrace();
								}

								String newQuery;

								if (!matchString.toLowerCase().startsWith(queryText.getText().toLowerCase())) {

									e.doit = false;
									break;
								}

								int nextStop = queryText.getCharCount();
								boolean changed = false;

								for (int[] argRange : argRanges) {

									if (argRange[0] > nextStop) {
										nextStop = argRange[0];
										changed = true;
										break;
									}

									if (argRange[1] == 0)
										break;
								}

								if (match.isComplete() && nextStop == queryText.getCharCount()
										&& nextStop < matchString.length()) {
									nextStop = matchString.length();
									changed = true;
								}

								if (!changed) {
									e.doit = false;
									break;
								}

								newQuery = matchString.substring(0, nextStop);

								queryText.setText(newQuery);
								queryText.setCaretOffset(queryText.getCharCount());
							}

							e.doit = false;
							break;
						}
					}
				});

				ignoredListener = new CloseOnIgnoreListener(shell);
				queryText.addFocusListener(ignoredListener);
			}
		}

		shell.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent evt) {

				cancelThreads();

				env.reset();

				
				 /* If the search box was closed by confirming a selection rather
				 * than canceling it, then the selected result should be
				 * re-applied fully (not just the preview).
				 */
				
				if (selectionConfirmed && selection != -1) {

					try {
						((JavaSnippetMatchEnvironment) env).applyMatch(matches.get(selection), true, getTotalHeight());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				if (selection != -1)
					sendUsageData();
			}
		});

		shell.pack();

		if (env instanceof JavaSnippetMatchEnvironment) {

			// Set the initial position of the search box to be right below the
			// cursor.
			Point anchor = ((JavaSnippetMatchEnvironment) env).getSearchBoxAnchor(getTotalHeight());
			shell.setLocation(anchor.x, anchor.y);
		}

		shell.open();

		matches = new ArrayList<MatchNode>();
		completeMatchThreads = new ArrayList<CompleteMatchThread>();
		noResultsYet = false;
		selection = -1;
		selectionConfirmed = false;
			
		shell.setFocus();
	}
	
	private void sendUsageData() {

		ISendFeedbackListener listener = new ISendFeedbackListener() {
			@Override
			public void sendFeedbackSucceeded() {
			}

			@Override
			public void sendFeedbackFailed(final String error) {
			}
		};

		if (!client.isWorking())
			client.startSendFeedback(queryText.getText(), matches.get(selection), null, 1, false, true, false,
					SnipMatchPlugin.getClientId(), selectionConfirmed, listener);
	}

	/**
	 * Gets the height of the entire search interface (query box + spacing +
	 * search results).
	 * 
	 * @return
	 */
	private int getTotalHeight() {

		int totalHeight = shell.getSize().y;
		/*if (matches != null && matches.size() != 0 && !matchTexts.get(0).isDisposed())
			totalHeight += matches.size() * matchTexts.get(0).getSize().y + 3;*/
		if (matches != null && matches.size() != 0)
			totalHeight += idealHeight;
		return totalHeight;
	}

	/**
	 * Called when the contents of the query box have changed.
	 */
	private void handleTyping() {
		final String query = queryText.getText();
		client.cancelWork();
		/*
		 * If the query is empty, then reset all changes, cancel all on-going
		 * searches, and reset the search box position.
		 */
		if (query.isEmpty()) {
			clearMatches();
			cancelThreads();
			env.reset();

			shell.setLocation(shell.getLocation().x,
					((JavaSnippetMatchEnvironment) env).getSearchBoxAnchor(getTotalHeight()).y);
			return;
		}

		/*
		 * If the query text field is not editable, then it means this was a
		 * programmatically invoked status message change.
		 */
		if (!queryText.getEditable())
			return;

		/*
		 * If an invalid character was entered, ignore it, fix the query, and
		 * ignore the next text change as well, because it will be due to the
		 * fix.
		 */
		if (queryText.getText().contains("\t") || queryText.getText().contains("\r")
				|| queryText.getText().contains("\n")) {

			ignoreTextChange = true;

			queryText.setText(queryText.getText().replace("\t", "").replace("\r", "").replace("\n", ""));

			queryText.setSelectionRange(queryText.getCharCount(), 0);

			return;
		}

		if (ignoreTextChange) {
			ignoreTextChange = false;
			return;
		}

		// Set the tool tip to the query, in case the query doesn't fit in the
		// search box.
		queryText.setToolTipText(query);

		// Just in case? Don't remember what this was for, but it doesn't
		// hurt...
		if (query.endsWith(System.getProperty("line.separator")))
			return;

		
		// Following is remote search work model
		client.startSearch(query, env, new ISearchListener() {	
			@Override
			public void searchSucceeded() {
				// Waiting for shell thread finish all the other jobs.
				while (!completeMatchThreads.isEmpty()) {
					try {
						Thread.sleep(5);
					} catch (InterruptedException e) {
					}
				}
				PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

					@Override
					public void run() {
						// Search finished without any results.
						if (noResultsYet) {

							env.reset();
							clearMatches();
							shell.setLocation(shell.getLocation().x,
									((JavaSnippetMatchEnvironment) env).getSearchBoxAnchor(getTotalHeight()).y);
						} else {
							sortMatches();
							displayMatches();
							
							 /** If the user already pressed Enter before the
							 * search finished, then select and apply the first
							 * result automatically.*/
							if (selectionConfirmed)
								insertSelectCodeSnip();
						}
					}
				});
			}

			@Override
			public void searchFailed(final String error) {

				PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

					@Override
					public void run() {

						MessageBox popup = new MessageBox(PlatformUI.getWorkbench().getActiveWorkbenchWindow()
								.getShell(), SWT.ICON_ERROR | SWT.OK | SWT.APPLICATION_MODAL);

						popup.setText("SnipMatch");
						popup.setMessage(error);
						popup.open();
					}
				});
			}

			@Override
			public void matchFound(final MatchNode match) {
				// When a match is found, generate completions from it, and add
				// them.

				final CompleteMatchThread matchCompleter = new CompleteMatchThread(env, match);

				ICompleteMatchListener listener = new ICompleteMatchListener() {

					@Override
					public void completionFound(MatchNode match) {
						addMatch(match);
					}

					@Override
					public void completionFinished() {
						completeMatchThreads.remove(matchCompleter);
					}
				};

				matchCompleter.setListener(listener);
				PlatformUI.getWorkbench().getDisplay().asyncExec(matchCompleter);
				
				completeMatchThreads.add(matchCompleter);
			}
		});

		noResultsYet = true;
	}

	/**
	 * Puts all the empty matches (no arguments filled in) on the bottom.
	 */
	private void sortMatches() {

		int place = 0;

		for (int i = 0; i < matches.size(); i++) {

			EffectMatchNode match = (EffectMatchNode) matches.get(place);

			if (match.isEmpty()) {
				matches.remove(match);
				matches.add(match);
			} else
				place++;
		}
	}

	private void cancelThreads() {

		client.cancelWork();

		for (CompleteMatchThread completeMatchThread : completeMatchThreads) {
			completeMatchThread.cancel();
		}

		completeMatchThreads.clear();
	}

	/**
	 * Adds a completed match result to to result list.
	 * 
	 * @param match
	 */
	private void addMatch(MatchNode match) {

		// If this is the first result of a search, then clear everything from
		// the previous search.
		if (noResultsYet) {
			clearMatches();
			noResultsYet = false;
		}

		// Prevent duplicates or extras.
		if (matches.contains(match) || matches.size() >= maxMatches)
			return;

		matches.add(match);
	}
	
	private Image resultImage = AbstractUIPlugin.imageDescriptorFromPlugin(SnipMatchPlugin.PLUGIN_ID, "images/flag.gif").createImage();
	
	private void resultDisplayTableSelection(Table resultDisplayTable, int type){
		if(resultDisplayTable != null && !resultDisplayTable.isDisposed()){
			int current = resultDisplayTable.getSelectionIndex();
			int count = resultDisplayTable.getItemCount();
			switch(type){
				case 0:{
					resultDisplayTable.setSelection(0);
					resultOverviewDisplay(0);
					break;
				}
				case -1:{
					int turn = (count + current - 1) % count;
					resultDisplayTable.setSelection(turn);
					resultOverviewDisplay(turn);
					break;
				}
				case 1:{
					int turn = (current + 1) % count;
					resultDisplayTable.setSelection(turn);
					resultOverviewDisplay(turn);
					break;
				}
				default:
					resultDisplayTable.setSelection(current);
					resultOverviewDisplay(current);
			}
		}
	}
	
	/**
	 * Search result overview display canvas
	 * 
	 * @param selection
	 * 
	 * TODO: add resize feature for resultoverview shell after mouse click event
	 */
	private void resultOverviewDisplay(int selection){
		if(resultOverviewShell  == null || resultOverviewShell.isDisposed()){
			resultOverviewShell = new Shell(shell, SWT.BORDER);
			resultOverviewShell.setBackground(matchSelectOverviewBackColor);
			resultOverviewShell.setSize(idealWidth, idealHeight);
			resultOverviewShell.setLayout(new FillLayout(SWT.VERTICAL));
			resultOverviewShell.addFocusListener(ignoredListener);
		}
		
		if(resultOverviewPanel == null || resultOverviewPanel.isDisposed()){
			resultOverviewPanel = new StyledText(resultOverviewShell,  SWT.WRAP|SWT.BORDER|SWT.H_SCROLL|SWT.V_SCROLL);
			resultOverviewPanel.setEditable(false);
			//resultOverviewPanel = new Label(resultOverviewShell,  SWT.WRAP|SWT.BORDER|SWT.V_SCROLL);
			resultOverviewPanel.setBackground(matchSelectOverviewBackColor);
			resultOverviewPanel.addFocusListener(ignoredListener);
			resultOverviewPanel.setAlwaysShowScrollBars(false);
		}
		
		MatchNode match = matches.get(selection);
		if (match instanceof EffectMatchNode){
			EffectMatchNode node = (EffectMatchNode) match;
			Point point = resultDisplayShell.getLocation();
			Object overViewText = ((JavaSnippetMatchEnvironment) env).evaluateMatchNodeOverview(node);
			if(overViewText != null){
				resultOverviewPanel.setText(overViewText.toString());
			}
			//resultOverviewPanel.setText(node.getEffect().getCode());
			
			
			resultOverviewShell.setLocation(point.x + resultDisplayShell.getSize().x, point.y);
			resultOverviewShell.open();
		}
		
		queryText.setFocus();
	}
	
	private KeyListener resultDisplayKeyListener = new KeyListener(){

		@Override
		public void keyPressed(KeyEvent e) {
			if(e.keyCode == SWT.ARROW_UP )
			{
				resultDisplayTableSelection(resultDisplayTable, -1);
			}
			
			if(e.keyCode == SWT.ARROW_DOWN)
			{
				resultDisplayTableSelection(resultDisplayTable, 1);
			}
		}

		@Override
		public void keyReleased(KeyEvent e) {
			if(e.keyCode == SWT.CR){
				insertSelectCodeSnip();
			}
		}
		
	};
	
	private MouseListener resultDisplayMouseListener = new MouseListener(){

		@Override
		public void mouseDoubleClick(MouseEvent e) {
			insertSelectCodeSnip();
		}

		@Override
		public void mouseDown(MouseEvent e) {
			//update current selection status
			resultDisplayTableSelection(resultDisplayTable, Integer.MAX_VALUE);
		}

		@Override
		public void mouseUp(MouseEvent e) {
			resultDisplayTableSelection(resultDisplayTable, Integer.MAX_VALUE);
			queryText.setFocus();
		}
		
	};
	
	private void insertSelectCodeSnip(){
		int selection = resultDisplayTable.getSelectionIndex();
		// This signals that the user wishes to insert the
		// currently highlighted result.
		selectionConfirmed = true;
		MatchNode matchSelection = matches.get(selection);
		if (!client.isWorking() && completeMatchThreads.isEmpty()) {
			shell.close();
		}
		
		env.reset();
		try {
			((JavaSnippetMatchEnvironment) env).applyMatch(matchSelection, true, 10);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Updates the visual result listing based on the search results.
	 */
	private void displayMatches() {
		if(resultDisplayShell == null || resultDisplayShell.isDisposed()){
			resultDisplayShell = new Shell(shell, SWT.BORDER|SWT.RESIZE);
			resultDisplayShell.setBackground(matchSelectBackColor);
			resultDisplayShell.setSize(idealWidth, idealHeight);
			resultDisplayShell.setLayout(new FillLayout(SWT.VERTICAL));
		}
		
		if(resultDisplayTable == null || resultDisplayTable.isDisposed()){
			resultDisplayTable = new Table(resultDisplayShell, SWT.SINGLE|SWT.V_SCROLL|SWT.H_SCROLL);
			resultDisplayTable.setBackground(matchSelectBackColor);
			resultDisplayTable.setLinesVisible(false);	
			TableColumn col = new TableColumn(resultDisplayTable, SWT.LEFT);  
			col.setText("");
			
		}
		
		resultDisplayTable.addKeyListener(resultDisplayKeyListener);
		resultDisplayTable.addMouseListener(resultDisplayMouseListener);
		resultDisplayTable.setRedraw(false);
		try{
			resultDisplayTable.removeAll();
			for (int i = 0; i < matches.size(); i++) {
				MatchNode match = matches.get(i);
				ArrayList<int[]> argRanges = new ArrayList<int[]>();
				ArrayList<int[]> blankArgRanges = new ArrayList<int[]>();
				String dispStr = "";
				try{
						dispStr = buildMatchString(match, argRanges, blankArgRanges, true, 0);
				}catch(Exception e){
					e.printStackTrace();
					matches.remove(i);
					i--;
					continue;
				}
				TableItem item = new TableItem(resultDisplayTable, SWT.NONE);
				item.setText(new String[]{dispStr});
				item.setImage(resultImage);
			}
			resultDisplayTable.setFont(searchFont);
			resultDisplayTable.getColumn(0).pack();
			
			if (env instanceof JavaSnippetMatchEnvironment) {
				Point anchor = ((JavaSnippetMatchEnvironment) env).getSearchBoxAnchor(getTotalHeight());
				resultDisplayShell.setLocation(anchor.x, anchor.y + 35);
			}
		}finally { 
			resultDisplayTable.setRedraw(true); 
		} 
		resultDisplayTable.addFocusListener(ignoredListener);
		resultDisplayShell.addFocusListener(ignoredListener);
		resultDisplayShell.open();
		resultDisplayTable.setFocus();
		if(matches.size() > 0)
			resultDisplayTableSelection(resultDisplayTable, 0);
	}

	/**
	 * Gets a string representation of a match, along with some other
	 * information.
	 * 
	 * @param match
	 *            The match.
	 * @param argRanges
	 *            A list to be filled with the ranges of non-empty arguments.
	 * @param blankArgRanges
	 *            A list to be filled with the ranges of empty arguments.
	 * @param showBlanks
	 *            Whether or not to show place-holders for empty arguments.
	 * @param length
	 *            The current length of the string. Used for recursion.
	 * @return A string representation of the match.
	 */
	private String buildMatchString(MatchNode match, ArrayList<int[]> argRanges, ArrayList<int[]> blankArgRanges,
			boolean showBlanks, int length) throws Exception{

		if (match instanceof EffectMatchNode) {

			StringBuilder sb = new StringBuilder();
			String[] tokens = ((EffectMatchNode) match).getPattern().split("\\s+");

			if (length != 0 && showBlanks)
				sb.append("(");

			for (String token : tokens) {

				if (token.startsWith("$")) {

					MatchNode child = ((EffectMatchNode) match).getChild(token.substring(1));
					sb.append(buildMatchString(child, argRanges, blankArgRanges, showBlanks, length + sb.length())
							+ " ");
				} else
					sb.append(token + " ");
			}

			sb.deleteCharAt(sb.length() - 1);

			if (length != 0 && showBlanks)
				sb.append(")");

			return sb.toString();
		} else {

			ArgumentMatchNode argNode = (ArgumentMatchNode) match;

			String token = argNode.getArgument();

			if (token.isEmpty()) {

				if (showBlanks)
					token = "<" + argNode.getParameter().getName() + ">";

				blankArgRanges.add(new int[] { length, token.length() });
			}

			argRanges.add(new int[] { length, token.length() });
			return token;
		}
	}

	/**
	 * Clears the match list, and also the visual listings.
	 */
	private void clearMatches() {
		if(resultDisplayTable != null && !resultDisplayTable.isDisposed())
			resultDisplayTable.removeAll();
		if(resultOverviewShell != null && !resultOverviewShell.isDisposed()){
			resultOverviewShell.close();
		}

		if (buttonBar != null && !buttonBar.isDisposed())
			buttonBar.dispose();

		matches.clear();
		selection = -1;
		queryText.setFocus();
	}

	public void hide() {

		if (!shell.isDisposed())
			shell.close();
	}

	/**
	 * Lock the search box while logging in, and display a message.
	 */
	public void lock() {

		if (queryText != null) {

			queryText.setEditable(false);
			queryText.setText("Logging in...");
			queryText.setBackground(queryDisabledBackColor);
		}
	}

	/**
	 * Release the lock on the search box once logged in.
	 */
	public void release() {

		if (queryText != null) {

			queryText.setText("");
			queryText.setEditable(true);
			queryText.setBackground(queryBackColor);
		}
	}
}
