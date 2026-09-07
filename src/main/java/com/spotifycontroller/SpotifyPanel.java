package com.spotifycontroller;

import java.awt.BorderLayout;
import java.awt.BasicStroke;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.TransferHandler;
import javax.swing.WindowConstants;
import javax.swing.plaf.basic.BasicSliderUI;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

final class SpotifyPanel extends PluginPanel
{
	private static final long serialVersionUID = 1L;
	private static final int ARTWORK_SIZE = 88;
	private static final int QUEUE_ITEM_HEIGHT = 50;
	private static final int SEARCH_ITEM_HEIGHT = 56;
	private static final int SEARCH_ARTWORK_SIZE = 44;
	private static final int MAXIMUM_QUEUE_ITEMS = 10;
	private static final String PLAYER_VIEW = "player";
	private static final String SEARCH_VIEW = "search";
	private static final String SETTINGS_VIEW = "settings";

	private static final Color DEFAULT_ACCENT = new Color(29, 185, 84);
	private static final Color DEFAULT_OVERLAY_PROGRESS_TRACK = new Color(118, 137, 148);
	private static final Color DEFAULT_OVERLAY_PLAYBACK_PROGRESS = new Color(238, 238, 238);
	private static final Color DEFAULT_OVERLAY_BACKGROUND = new Color(14, 14, 14, 118);
	private static final Color DEFAULT_OVERLAY_PLAYBACK_BUTTON = new Color(238, 238, 238);
	private static final Color OVERLAY_OFF_BACKGROUND = new Color(61, 31, 31);
	private static final Color OVERLAY_OFF_HOVER = new Color(78, 38, 38);
	private static final Color OVERLAY_OFF_TEXT = new Color(255, 190, 190);
	private static final Color OVERLAY_OFF_BORDER = new Color(169, 78, 78);
	private static final Color PANEL_BACKGROUND = new Color(24, 24, 24);
	private static final Color CARD_BACKGROUND = new Color(14, 14, 14);
	private static final Color CARD_BORDER = new Color(66, 66, 66);
	private static final Color PRIMARY_TEXT = new Color(238, 238, 238);
	private static final Color SECONDARY_TEXT = new Color(180, 180, 180);
	private static final Color BUTTON_BACKGROUND = new Color(31, 31, 31);
	private static final Color BUTTON_DISABLED = new Color(25, 25, 25);
	private static final Color PROGRESS_TRACK = new Color(78, 88, 94);
	private static final Icon PREVIOUS_TRANSPORT_ICON = new TransportIcon(TransportIcon.PREVIOUS);
	private static final Icon PLAY_TRANSPORT_ICON = new TransportIcon(TransportIcon.PLAY);
	private static final Icon PAUSE_TRANSPORT_ICON = new TransportIcon(TransportIcon.PAUSE);
	private static final Icon NEXT_TRANSPORT_ICON = new TransportIcon(TransportIcon.NEXT);

	private final SpotifyControllerPlugin plugin;
	private final SpotifyControllerConfig config;
	private final Artwork artwork = new Artwork();
	private final JLabel sourceHeading = new JLabel("Spotify");
	private final JTextArea title = textArea(FontManager.getRunescapeBoldFont().deriveFont(19f));
	private final JTextArea artist = textArea(FontManager.getRunescapeFont().deriveFont(16f));
	private final JLabel playbackStatus = new JLabel("Idle");
	private final JLabel elapsedTime = new JLabel("0:00");
	private final JLabel remainingTime = new JLabel("-0:00", SwingConstants.RIGHT);
	private final SeekBar progress = new SeekBar();
	private final JLabel volumeValue = new JLabel("Unavailable", SwingConstants.RIGHT);
	private final JSlider volume = new JSlider(0, 100, 0);
	private final JButton previous = new JButton(PREVIOUS_TRANSPORT_ICON);
	private final JButton playPause = new JButton(PLAY_TRANSPORT_ICON);
	private final JButton next = new JButton(NEXT_TRANSPORT_ICON);
	private final JButton shuffle = new JButton(new PlaybackModeIcon(true));
	private final JButton repeat = new JButton(new PlaybackModeIcon(false));
	private final JPanel spotifyModeControls = transparentPanel(new GridLayout(2, 1, 0, 8));
	private final JButton overlayToggle = new JButton("In-game overlay: ON");
	private final JButton overlaySourceToggle = new JButton("Source: ON");
	private final JButton overlayTitleToggle = new JButton("Song title: ON");
	private final JButton overlayArtistToggle = new JButton("Artist: ON");
	private final JButton overlayControlsToggle = new JButton("Playback Controls: ON");
	private final JButton overlayProgressToggle = new JButton("Playback Progress: ON");
	private final JButton overlayArtworkToggle = new JButton("Album Artwork: ON");
	private final JButton overlayLyricsToggle = new JButton("Lyrics: OFF");
	private final JLabel overlayLyricsLinesLabel = new JLabel("Lyric lines: 3");
	private final JButton overlayLyricsDecrease = new JButton("−");
	private final JButton overlayLyricsIncrease = new JButton("+");
	private final JButton overlayAccentButton = new JButton();
	private final JButton sidebarAccentButton = new JButton();
	private final JButton overlayProgressTrackButton = new JButton();
	private final JButton overlayPlaybackProgressButton = new JButton();
	private final JButton overlayBackgroundButton = new JButton();
	private final JButton overlayPlaybackButtonColorButton = new JButton();
	private final JButton connect = new JButton("Connect Spotify");
	private final JButton disconnect = new JButton("Disconnect");
	private final JButton refresh = new JButton("Refresh");
	private final JLabel spotifyApiStatus = new JLabel("Spotify API: waiting…");
	private final JButton about = new JButton("About");
	private final JButton playerTab = new JButton("Playback");
	private final JButton searchTab = new JButton("Search");
	private final JButton settingsTab = new JButton("Settings");
	private final CardLayout sidebarCardLayout = new CardLayout();
	private final JPanel sidebarCards = transparentPanel(sidebarCardLayout);
	private final JPanel connectedControls = transparentPanel(new GridLayout(1, 2, 7, 0));
	private final DefaultListModel<SpotifyQueueItem> queueModel = new DefaultListModel<>();
	private final JList<SpotifyQueueItem> queueList = new JList<>(queueModel);
	private final JLabel emptyQueue = new JLabel("No songs are currently queued.", SwingConstants.CENTER);
	private final RoundedPanel queueCard = new RoundedPanel();
	private final JTextField searchQuery = new JTextField();
	private final JButton searchButton = new JButton("Search");
	private final DefaultListModel<SpotifySearchResult> searchModel = new DefaultListModel<>();
	private final JList<SpotifySearchResult> searchList = new JList<>(searchModel);
	private final Map<String, BufferedImage> searchArtwork = new HashMap<>();
	private final JButton playSearchResult = new JButton("Play selected");
	private final JLabel searchStatus = new JLabel("Search Spotify for a song.");
	private JPanel connectionCard;
	private final Timer progressTimer;
	private final Timer volumeCommitTimer;
	private JDialog aboutDialog;
	private int hoveredQueueIndex = -1;

	private SpotifyPlaybackState playbackState = SpotifyPlaybackState.idle();
	private boolean connected;
	private boolean pluginEnabled;
	private boolean hasClientId;
	private boolean overlayVisible;
	private boolean overlaySourceVisible;
	private boolean overlayTitleVisible;
	private boolean overlayArtistVisible;
	private boolean overlayControlsVisible;
	private boolean overlayProgressVisible;
	private boolean overlayArtworkVisible;
	private boolean overlayLyricsVisible;
	private int overlayLyricsLines = 3;
	private boolean systemMediaMode;
	private boolean updatingVolume;
	private boolean sidebarSeeking;
	private boolean searchBusy;
	private String selectedSidebarView = PLAYER_VIEW;

	SpotifyPanel(SpotifyControllerPlugin plugin, SpotifyControllerConfig config)
	{
		super(false);
		this.plugin = plugin;
		this.config = config;
		volumeCommitTimer = new Timer(140, event -> plugin.setVolume(volume.getValue()));
		volumeCommitTimer.setRepeats(false);
		setLayout(new BorderLayout());
		setBackground(PANEL_BACKGROUND);

		add(createSidebarTabs(), BorderLayout.NORTH);
		sidebarCards.add(createPlayerView(), PLAYER_VIEW);
		sidebarCards.add(createSearchView(), SEARCH_VIEW);
		sidebarCards.add(createSettingsView(), SETTINGS_VIEW);
		add(sidebarCards, BorderLayout.CENTER);

		JPanel footer = transparentPanel(new BorderLayout());
		footer.setBorder(BorderFactory.createEmptyBorder(0, 8, 12, 8));
		footer.add(createAboutCard(), BorderLayout.CENTER);
		add(footer, BorderLayout.SOUTH);

		showSidebarView(PLAYER_VIEW);
		progressTimer = new Timer(1000, event -> updateProgress());
		progressTimer.start();
		updateToggleAppearance(overlayToggle, overlayVisible, "In-game overlay");
		updateToggleAppearance(overlaySourceToggle, overlaySourceVisible, "Source");
		updateToggleAppearance(overlayTitleToggle, overlayTitleVisible, "Song title");
		updateToggleAppearance(overlayArtistToggle, overlayArtistVisible, "Artist");
		updateToggleAppearance(overlayControlsToggle, overlayControlsVisible, "Playback Controls");
		updateToggleAppearance(overlayProgressToggle, overlayProgressVisible, "Playback Progress");
		updateToggleAppearance(overlayArtworkToggle, overlayArtworkVisible, "Album Artwork");
		updateToggleAppearance(overlayLyricsToggle, overlayLyricsVisible, "Lyrics");
		refreshTheme();
		updateQueueSize();
		updateButtons();
	}

	private JPanel createSidebarTabs()
	{
		JPanel wrapper = transparentPanel(new BorderLayout());
		wrapper.setBorder(BorderFactory.createEmptyBorder(10, 8, 0, 8));
		JPanel tabs = transparentPanel(new GridLayout(1, 3, 5, 0));

		configureSidebarTab(playerTab);
		configureSidebarTab(searchTab);
		configureSidebarTab(settingsTab);
		playerTab.addActionListener(event -> showSidebarView(PLAYER_VIEW));
		searchTab.addActionListener(event -> showSidebarView(SEARCH_VIEW));
		settingsTab.addActionListener(event -> showSidebarView(SETTINGS_VIEW));
		tabs.add(playerTab);
		tabs.add(searchTab);
		tabs.add(settingsTab);
		wrapper.add(tabs, BorderLayout.CENTER);
		return wrapper;
	}

	private JPanel createPlayerView()
	{
		JPanel content = transparentPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBorder(BorderFactory.createEmptyBorder(10, 8, 12, 8));

		content.add(createNowPlayingCard());
		content.add(Box.createRigidArea(new Dimension(0, 9)));
		configureQueueCard();
		content.add(queueCard);
		content.add(Box.createVerticalGlue());
		content.add(Box.createRigidArea(new Dimension(0, 9)));
		connectionCard = createConnectionCard();
		content.add(connectionCard);
		return content;
	}

	private JPanel createSearchView()
	{
		JPanel content = transparentPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBorder(BorderFactory.createEmptyBorder(14, 8, 12, 8));

		JLabel heading = new JLabel("Find a song");
		heading.setForeground(PRIMARY_TEXT);
		heading.setFont(FontManager.getRunescapeBoldFont().deriveFont(18f));
		heading.setAlignmentX(LEFT_ALIGNMENT);
		content.add(heading);
		content.add(Box.createRigidArea(new Dimension(0, 4)));

		JLabel hint = new JLabel("Search Spotify, then start the selected result.");
		hint.setForeground(SECONDARY_TEXT);
		hint.setFont(FontManager.getRunescapeSmallFont().deriveFont(13f));
		hint.setAlignmentX(LEFT_ALIGNMENT);
		content.add(hint);
		content.add(Box.createRigidArea(new Dimension(0, 10)));

		searchQuery.setFont(FontManager.getRunescapeFont().deriveFont(15f));
		searchQuery.setBackground(CARD_BACKGROUND);
		searchQuery.setForeground(PRIMARY_TEXT);
		searchQuery.setCaretColor(PRIMARY_TEXT);
		searchQuery.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(CARD_BORDER),
			BorderFactory.createEmptyBorder(5, 7, 5, 7)));
		searchQuery.setToolTipText("Song, artist, album, or Spotify search terms");
		searchQuery.setMaximumSize(new Dimension(Integer.MAX_VALUE, 31));
		searchQuery.setAlignmentX(LEFT_ALIGNMENT);
		searchQuery.addActionListener(event -> submitSearch());
		content.add(searchQuery);
		content.add(Box.createRigidArea(new Dimension(0, 7)));

		searchButton.setAlignmentX(LEFT_ALIGNMENT);
		searchButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
		searchButton.addActionListener(event -> submitSearch());
		styleAccentButton(searchButton);
		content.add(searchButton);
		content.add(Box.createRigidArea(new Dimension(0, 9)));

		searchStatus.setForeground(SECONDARY_TEXT);
		searchStatus.setFont(FontManager.getRunescapeSmallFont().deriveFont(13f));
		searchStatus.setAlignmentX(LEFT_ALIGNMENT);
		searchStatus.setToolTipText(searchStatus.getText());
		content.add(searchStatus);
		content.add(Box.createRigidArea(new Dimension(0, 7)));

		searchList.setCellRenderer(new SearchResultRenderer());
		searchList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		searchList.setFixedCellHeight(SEARCH_ITEM_HEIGHT);
		searchList.setBackground(CARD_BACKGROUND);
		searchList.setForeground(PRIMARY_TEXT);
		searchList.setSelectionBackground(buttonHoverBackground());
		searchList.setSelectionForeground(PRIMARY_TEXT);
		searchList.addListSelectionListener(event ->
		{
			if (!event.getValueIsAdjusting())
			{
				updateButtons();
			}
		});
		searchList.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent event)
			{
				if (event.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(event))
				{
					playSelectedSearchResult();
				}
			}
		});

		JScrollPane results = new JScrollPane(searchList);
		results.setAlignmentX(LEFT_ALIGNMENT);
		results.setBorder(BorderFactory.createLineBorder(CARD_BORDER));
		results.getViewport().setBackground(CARD_BACKGROUND);
		results.setPreferredSize(new Dimension(1, 280));
		results.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		content.add(results);
		content.add(Box.createRigidArea(new Dimension(0, 8)));

		playSearchResult.setAlignmentX(LEFT_ALIGNMENT);
		playSearchResult.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
		playSearchResult.addActionListener(event -> playSelectedSearchResult());
		styleAccentButton(playSearchResult);
		content.add(playSearchResult);
		return content;
	}

	private void submitSearch()
	{
		String query = searchQuery.getText() == null ? "" : searchQuery.getText().trim();
		if (query.isEmpty())
		{
			setSearchError("Enter a song or artist first.");
			searchQuery.requestFocusInWindow();
			return;
		}
		setSearchLoading(query);
		plugin.searchTracks(query);
	}

	private void playSelectedSearchResult()
	{
		SpotifySearchResult selected = searchList.getSelectedValue();
		if (selected == null || !playSearchResult.isEnabled())
		{
			return;
		}
		searchBusy = true;
		setSearchStatus("Starting “" + selected.getTitle() + "”…");
		updateButtons();
		plugin.playSearchResult(selected);
	}

	private JPanel createSettingsView()
	{
		JPanel content = transparentPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBorder(BorderFactory.createEmptyBorder(14, 8, 12, 8));

		JLabel heading = new JLabel("In-game overlay");
		heading.setForeground(PRIMARY_TEXT);
		heading.setFont(FontManager.getRunescapeBoldFont().deriveFont(18f));
		heading.setAlignmentX(LEFT_ALIGNMENT);
		content.add(heading);
		content.add(Box.createRigidArea(new Dimension(0, 4)));

		JLabel hint = new JLabel("Choose what appears over the game.");
		hint.setForeground(SECONDARY_TEXT);
		hint.setFont(FontManager.getRunescapeSmallFont().deriveFont(14f));
		hint.setAlignmentX(LEFT_ALIGNMENT);
		content.add(hint);
		content.add(Box.createRigidArea(new Dimension(0, 10)));
		content.add(createOverlaySettingsCard());
		content.add(Box.createRigidArea(new Dimension(0, 14)));

		JLabel themeHeading = new JLabel("Theme colors");
		themeHeading.setForeground(PRIMARY_TEXT);
		themeHeading.setFont(FontManager.getRunescapeBoldFont().deriveFont(18f));
		themeHeading.setAlignmentX(LEFT_ALIGNMENT);
		content.add(themeHeading);
		content.add(Box.createRigidArea(new Dimension(0, 4)));

		JLabel themeHint = new JLabel("Choose separate accent and progress colors.");
		themeHint.setForeground(SECONDARY_TEXT);
		themeHint.setFont(FontManager.getRunescapeSmallFont().deriveFont(13f));
		themeHint.setAlignmentX(LEFT_ALIGNMENT);
		content.add(themeHint);
		content.add(Box.createRigidArea(new Dimension(0, 10)));
		content.add(createThemeSettingsCard());
		content.add(Box.createVerticalGlue());
		return content;
	}

	private RoundedPanel createNowPlayingCard()
	{
		RoundedPanel card = new RoundedPanel();
		card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
		card.setAlignmentX(LEFT_ALIGNMENT);
		card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 390));

		sourceHeading.setForeground(sidebarAccent());
		sourceHeading.setFont(FontManager.getRunescapeBoldFont().deriveFont(19f));
		playbackStatus.setFont(FontManager.getRunescapeSmallFont().deriveFont(14f));
		playbackStatus.setForeground(SECONDARY_TEXT);
		JPanel headingRow = transparentPanel(new BorderLayout(8, 0));
		headingRow.setAlignmentX(LEFT_ALIGNMENT);
		headingRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
		headingRow.add(sourceHeading, BorderLayout.WEST);
		headingRow.add(playbackStatus, BorderLayout.EAST);
		card.add(headingRow);
		card.add(Box.createRigidArea(new Dimension(0, 10)));

		artwork.setToolTipText("Cover art supplied by the active media source");
		JPanel artworkRow = transparentPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
		artworkRow.setAlignmentX(LEFT_ALIGNMENT);
		artworkRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, ARTWORK_SIZE));
		artworkRow.add(artwork);

		configureSpotifyModeButton(shuffle);
		shuffle.setToolTipText("Shuffle: Off");
		shuffle.addActionListener(event -> plugin.toggleShuffle(playbackState.isShuffleEnabled()));
		configureSpotifyModeButton(repeat);
		repeat.setToolTipText("Repeat: Off");
		repeat.addActionListener(event -> plugin.cycleRepeatMode(playbackState.getRepeatMode()));
		spotifyModeControls.setPreferredSize(new Dimension(32, 70));
		spotifyModeControls.setMaximumSize(new Dimension(32, 70));
		spotifyModeControls.add(shuffle);
		spotifyModeControls.add(repeat);
		artworkRow.add(spotifyModeControls);
		card.add(artworkRow);
		card.add(Box.createRigidArea(new Dimension(0, 10)));

		title.setText("Nothing playing");
		title.setRows(2);
		artist.setForeground(SECONDARY_TEXT);
		addFullWidth(card, title, 52);
		card.add(Box.createRigidArea(new Dimension(0, 3)));
		addFullWidth(card, artist, 32);
		card.add(Box.createRigidArea(new Dimension(0, 10)));

		progress.setMaximum(1);
		progress.setValue(0);
		progress.setAlignmentX(LEFT_ALIGNMENT);
		progress.setPreferredSize(new Dimension(1, 18));
		progress.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
		progress.setToolTipText("Click or drag to seek");
		MouseAdapter progressMouseHandler = new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent event)
			{
				if (!SwingUtilities.isLeftMouseButton(event) || !progress.isEnabled() ||
					playbackState.getDurationMs() <= 0)
				{
					return;
				}
				sidebarSeeking = true;
				progress.setHovered(true);
				updateSidebarSeekPreview(event.getX());
			}

			@Override
			public void mouseDragged(MouseEvent event)
			{
				if (sidebarSeeking)
				{
					updateSidebarSeekPreview(event.getX());
				}
			}

			@Override
			public void mouseReleased(MouseEvent event)
			{
				if (!sidebarSeeking)
				{
					return;
				}
				updateSidebarSeekPreview(event.getX());
				sidebarSeeking = false;
				plugin.seekTo(progress.getValue());
			}

			@Override
			public void mouseEntered(MouseEvent event)
			{
				progress.setHovered(true);
			}

			@Override
			public void mouseExited(MouseEvent event)
			{
				if (!sidebarSeeking)
				{
					progress.setHovered(false);
				}
			}
		};
		progress.addMouseListener(progressMouseHandler);
		progress.addMouseMotionListener(progressMouseHandler);
		card.add(progress);
		card.add(Box.createRigidArea(new Dimension(0, 4)));

		elapsedTime.setFont(FontManager.getRunescapeSmallFont().deriveFont(13f));
		remainingTime.setFont(FontManager.getRunescapeSmallFont().deriveFont(13f));
		elapsedTime.setForeground(SECONDARY_TEXT);
		remainingTime.setForeground(SECONDARY_TEXT);
		JPanel timeRow = transparentPanel(new BorderLayout());
		timeRow.setAlignmentX(LEFT_ALIGNMENT);
		timeRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 19));
		timeRow.add(elapsedTime, BorderLayout.WEST);
		timeRow.add(remainingTime, BorderLayout.EAST);
		card.add(timeRow);
		card.add(Box.createRigidArea(new Dimension(0, 8)));

		JLabel volumeLabel = new JLabel("Volume");
		volumeLabel.setFont(FontManager.getRunescapeSmallFont().deriveFont(14f));
		volumeLabel.setForeground(SECONDARY_TEXT);
		volumeValue.setFont(FontManager.getRunescapeSmallFont().deriveFont(14f));
		volumeValue.setForeground(SECONDARY_TEXT);
		JPanel volumeHeading = transparentPanel(new BorderLayout());
		volumeHeading.setAlignmentX(LEFT_ALIGNMENT);
		volumeHeading.setMaximumSize(new Dimension(Integer.MAX_VALUE, 19));
		volumeHeading.add(volumeLabel, BorderLayout.WEST);
		volumeHeading.add(volumeValue, BorderLayout.EAST);
		card.add(volumeHeading);
		card.add(Box.createRigidArea(new Dimension(0, 2)));

		volume.setOpaque(false);
		volume.setFocusable(true);
		volume.setPaintTicks(false);
		volume.setPaintLabels(false);
		volume.setAlignmentX(LEFT_ALIGNMENT);
		volume.setPreferredSize(new Dimension(1, 18));
		volume.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
		volume.setToolTipText("Volume for the active media session");
		volume.setUI(new VolumeSliderUI(volume));
		volume.addChangeListener(event ->
		{
			if (updatingVolume || !volume.isEnabled())
			{
				return;
			}
			volumeValue.setText(volume.getValue() + "%");
			volumeCommitTimer.restart();
		});
		card.add(volume);
		card.add(Box.createRigidArea(new Dimension(0, 10)));

		JPanel controls = transparentPanel(new GridLayout(1, 3, 7, 0));
		controls.setAlignmentX(LEFT_ALIGNMENT);
		controls.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
		previous.setToolTipText("Previous track");
		playPause.setToolTipText("Play or pause");
		next.setToolTipText("Next track");
		previous.addActionListener(event -> plugin.previous());
		playPause.addActionListener(event -> plugin.togglePlayPause(playbackState.isPlaying()));
		next.addActionListener(event -> plugin.next());
		styleDarkButton(previous);
		styleDarkButton(playPause);
		styleDarkButton(next);
		previous.setFont(FontManager.getRunescapeBoldFont().deriveFont(15f));
		playPause.setFont(FontManager.getRunescapeBoldFont().deriveFont(15f));
		next.setFont(FontManager.getRunescapeBoldFont().deriveFont(15f));
		controls.add(previous);
		controls.add(playPause);
		controls.add(next);
		card.add(controls);
		return card;
	}

	private RoundedPanel createOverlaySettingsCard()
	{
		RoundedPanel card = new RoundedPanel();
		card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
		card.setAlignmentX(LEFT_ALIGNMENT);
		card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 390));

		configureToggleButton(overlayToggle);
		overlayToggle.setToolTipText("Show or hide the Spotify panel over the game");
		overlayToggle.addChangeListener(event ->
			updateToggleAppearance(overlayToggle, overlayVisible, "In-game overlay"));
		overlayToggle.addActionListener(event ->
		{
			setOverlayVisible(!overlayVisible);
			plugin.setGameOverlayVisible(overlayVisible);
		});
		card.add(overlayToggle);
		card.add(Box.createRigidArea(new Dimension(0, 7)));

		configureToggleButton(overlaySourceToggle);
		overlaySourceToggle.setToolTipText(
			"Show or hide Spotify and Playing/Paused in the in-game overlay");
		overlaySourceToggle.addChangeListener(event ->
			updateToggleAppearance(overlaySourceToggle, overlaySourceVisible, "Source"));
		overlaySourceToggle.addActionListener(event ->
		{
			setOverlaySourceVisible(!overlaySourceVisible);
			plugin.setGameOverlaySourceVisible(overlaySourceVisible);
		});
		card.add(overlaySourceToggle);
		card.add(Box.createRigidArea(new Dimension(0, 7)));

		configureToggleButton(overlayTitleToggle);
		overlayTitleToggle.setToolTipText("Show or hide the song or episode title in the in-game overlay");
		overlayTitleToggle.addChangeListener(event ->
			updateToggleAppearance(overlayTitleToggle, overlayTitleVisible, "Song title"));
		overlayTitleToggle.addActionListener(event ->
		{
			setOverlayTitleVisible(!overlayTitleVisible);
			plugin.setGameOverlayTitleVisible(overlayTitleVisible);
		});
		card.add(overlayTitleToggle);
		card.add(Box.createRigidArea(new Dimension(0, 7)));

		configureToggleButton(overlayArtistToggle);
		overlayArtistToggle.setToolTipText("Show or hide the artist or podcast name in the in-game overlay");
		overlayArtistToggle.addChangeListener(event ->
			updateToggleAppearance(overlayArtistToggle, overlayArtistVisible, "Artist"));
		overlayArtistToggle.addActionListener(event ->
		{
			setOverlayArtistVisible(!overlayArtistVisible);
			plugin.setGameOverlayArtistVisible(overlayArtistVisible);
		});
		card.add(overlayArtistToggle);
		card.add(Box.createRigidArea(new Dimension(0, 7)));

		configureToggleButton(overlayControlsToggle);
		overlayControlsToggle.setToolTipText("Show or hide playback buttons in the in-game overlay");
		overlayControlsToggle.addChangeListener(event ->
			updateToggleAppearance(overlayControlsToggle, overlayControlsVisible, "Playback Controls"));
		overlayControlsToggle.addActionListener(event ->
		{
			setOverlayControlsVisible(!overlayControlsVisible);
			plugin.setGameOverlayControlsVisible(overlayControlsVisible);
		});
		card.add(overlayControlsToggle);
		card.add(Box.createRigidArea(new Dimension(0, 7)));

		configureToggleButton(overlayProgressToggle);
		overlayProgressToggle.setToolTipText("Show or hide the seek bar and time labels in the in-game overlay");
		overlayProgressToggle.addChangeListener(event ->
			updateToggleAppearance(overlayProgressToggle, overlayProgressVisible, "Playback Progress"));
		overlayProgressToggle.addActionListener(event ->
		{
			setOverlayProgressVisible(!overlayProgressVisible);
			plugin.setGameOverlayProgressVisible(overlayProgressVisible);
		});
		card.add(overlayProgressToggle);
		card.add(Box.createRigidArea(new Dimension(0, 7)));

		configureToggleButton(overlayArtworkToggle);
		overlayArtworkToggle.setToolTipText("Show or hide album artwork in the in-game overlay");
		overlayArtworkToggle.addChangeListener(event ->
			updateToggleAppearance(overlayArtworkToggle, overlayArtworkVisible, "Album Artwork"));
		overlayArtworkToggle.addActionListener(event ->
		{
			setOverlayArtworkVisible(!overlayArtworkVisible);
			plugin.setGameOverlayArtworkVisible(overlayArtworkVisible);
		});
		card.add(overlayArtworkToggle);
		card.add(Box.createRigidArea(new Dimension(0, 7)));

		configureToggleButton(overlayLyricsToggle);
		overlayLyricsToggle.setToolTipText(
			"Show lyric lines; sends track metadata to LRCLIB");
		overlayLyricsToggle.addChangeListener(event ->
			updateToggleAppearance(overlayLyricsToggle, overlayLyricsVisible, "Lyrics"));
		overlayLyricsToggle.addActionListener(event ->
		{
			setOverlayLyricsVisible(!overlayLyricsVisible);
			plugin.setGameOverlayLyricsVisible(overlayLyricsVisible);
		});
		card.add(overlayLyricsToggle);
		card.add(Box.createRigidArea(new Dimension(0, 7)));

		overlayLyricsLinesLabel.setForeground(PRIMARY_TEXT);
		overlayLyricsLinesLabel.setFont(FontManager.getRunescapeSmallFont().deriveFont(14f));
		styleDarkButton(overlayLyricsDecrease);
		styleDarkButton(overlayLyricsIncrease);
		overlayLyricsDecrease.setToolTipText("Show fewer lyric lines");
		overlayLyricsIncrease.setToolTipText("Show more lyric lines");
		overlayLyricsDecrease.addActionListener(event ->
			plugin.setGameOverlayLyricsLines(overlayLyricsLines - 1));
		overlayLyricsIncrease.addActionListener(event ->
			plugin.setGameOverlayLyricsLines(overlayLyricsLines + 1));
		JPanel lyricLineRow = transparentPanel(new BorderLayout(6, 0));
		lyricLineRow.setAlignmentX(LEFT_ALIGNMENT);
		lyricLineRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
		JPanel lyricLineButtons = transparentPanel(new GridLayout(1, 2, 5, 0));
		lyricLineButtons.setPreferredSize(new Dimension(72, 31));
		lyricLineButtons.add(overlayLyricsDecrease);
		lyricLineButtons.add(overlayLyricsIncrease);
		lyricLineRow.add(overlayLyricsLinesLabel, BorderLayout.CENTER);
		lyricLineRow.add(lyricLineButtons, BorderLayout.EAST);
		card.add(lyricLineRow);
		return card;
	}

	private RoundedPanel createThemeSettingsCard()
	{
		RoundedPanel card = new RoundedPanel();
		card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
		card.setAlignmentX(LEFT_ALIGNMENT);
		card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 259));

		configureColorButton(overlayAccentButton);
		overlayAccentButton.setToolTipText("Choose the in-game overlay accent color");
		overlayAccentButton.addActionListener(event -> chooseAccentColor(true));
		card.add(overlayAccentButton);
		card.add(Box.createRigidArea(new Dimension(0, 7)));

		configureColorButton(sidebarAccentButton);
		sidebarAccentButton.setToolTipText("Choose the Spotify sidebar accent color");
		sidebarAccentButton.addActionListener(event -> chooseAccentColor(false));
		card.add(sidebarAccentButton);
		card.add(Box.createRigidArea(new Dimension(0, 7)));

		configureColorButton(overlayProgressTrackButton);
		overlayProgressTrackButton.setToolTipText("Choose the in-game overlay progress bar track color");
		overlayProgressTrackButton.addActionListener(event -> chooseOverlayProgressColor(true));
		card.add(overlayProgressTrackButton);
		card.add(Box.createRigidArea(new Dimension(0, 7)));

		configureColorButton(overlayPlaybackProgressButton);
		overlayPlaybackProgressButton.setToolTipText("Choose the played portion and position-marker color");
		overlayPlaybackProgressButton.addActionListener(event -> chooseOverlayProgressColor(false));
		card.add(overlayPlaybackProgressButton);
		card.add(Box.createRigidArea(new Dimension(0, 7)));

		configureColorButton(overlayBackgroundButton);
		overlayBackgroundButton.setToolTipText("Choose the in-game overlay background color");
		overlayBackgroundButton.addActionListener(event -> chooseOverlayBackgroundColor());
		card.add(overlayBackgroundButton);
		card.add(Box.createRigidArea(new Dimension(0, 7)));

		configureColorButton(overlayPlaybackButtonColorButton);
		overlayPlaybackButtonColorButton.setToolTipText(
			"Choose the previous, play/pause, and next icon color");
		overlayPlaybackButtonColorButton.addActionListener(event -> chooseOverlayPlaybackButtonColor());
		card.add(overlayPlaybackButtonColorButton);
		return card;
	}

	private void configureColorButton(JButton button)
	{
		button.setFont(FontManager.getRunescapeBoldFont().deriveFont(14f));
		button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
		button.setAlignmentX(LEFT_ALIGNMENT);
		styleDarkButton(button);
	}

	private void chooseAccentColor(boolean overlayAccent)
	{
		Color current = overlayAccent ? overlayAccent() : sidebarAccent();
		Color selected = JColorChooser.showDialog(
			this,
			overlayAccent ? "In-game overlay accent" : "Sidebar UI accent",
			current);
		if (selected == null)
		{
			return;
		}

		if (overlayAccent)
		{
			plugin.setOverlayAccentColor(selected);
		}
		else
		{
			plugin.setSidebarAccentColor(selected);
		}
	}

	private void chooseOverlayProgressColor(boolean track)
	{
		Color current = track ? overlayProgressTrack() : overlayPlaybackProgress();
		Color selected = JColorChooser.showDialog(
			this,
			track ? "Progress bar track" : "Playback progress",
			current);
		if (selected == null)
		{
			return;
		}

		if (track)
		{
			plugin.setOverlayProgressTrackColor(selected);
		}
		else
		{
			plugin.setOverlayPlaybackProgressColor(selected);
		}
	}

	private void chooseOverlayBackgroundColor()
	{
		Color current = overlayBackground();
		Color selected = JColorChooser.showDialog(this, "Overlay background", current);
		if (selected == null)
		{
			return;
		}

		plugin.setOverlayBackgroundColor(new Color(
			selected.getRed(), selected.getGreen(), selected.getBlue(), current.getAlpha()));
	}

	private void chooseOverlayPlaybackButtonColor()
	{
		Color selected = JColorChooser.showDialog(
			this, "Playback button icons", overlayPlaybackButtonColor());
		if (selected != null)
		{
			plugin.setOverlayPlaybackButtonColor(selected);
		}
	}

	private RoundedPanel createConnectionCard()
	{
		RoundedPanel card = new RoundedPanel();
		card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
		card.setAlignmentX(LEFT_ALIGNMENT);
		card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));

		spotifyApiStatus.setForeground(SECONDARY_TEXT);
		spotifyApiStatus.setFont(FontManager.getRunescapeSmallFont().deriveFont(13f));
		spotifyApiStatus.setAlignmentX(LEFT_ALIGNMENT);
		spotifyApiStatus.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
		spotifyApiStatus.setToolTipText(spotifyApiStatus.getText());
		card.add(spotifyApiStatus);
		card.add(Box.createRigidArea(new Dimension(0, 5)));

		connect.setFont(FontManager.getRunescapeBoldFont().deriveFont(15f));
		connect.addActionListener(event -> plugin.connectSpotify());
		connect.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
		connect.setAlignmentX(LEFT_ALIGNMENT);
		styleAccentButton(connect);
		card.add(connect);

		connectedControls.setAlignmentX(LEFT_ALIGNMENT);
		connectedControls.setMaximumSize(new Dimension(Integer.MAX_VALUE, 33));
		disconnect.addActionListener(event -> plugin.disconnectSpotify());
		refresh.addActionListener(event -> plugin.refreshSpotify());
		styleDarkButton(disconnect);
		styleDarkButton(refresh);
		disconnect.setFont(FontManager.getRunescapeBoldFont().deriveFont(15f));
		refresh.setFont(FontManager.getRunescapeBoldFont().deriveFont(15f));
		connectedControls.add(disconnect);
		connectedControls.add(refresh);
		card.add(connectedControls);
		return card;
	}

	private RoundedPanel createAboutCard()
	{
		RoundedPanel card = new RoundedPanel();
		card.setLayout(new BorderLayout());
		card.setAlignmentX(LEFT_ALIGNMENT);
		card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));

		about.setFont(FontManager.getRunescapeBoldFont().deriveFont(15f));
		about.setToolTipText("Plugin information");
		about.addActionListener(event -> showAboutDialog());
		styleDarkButton(about);
		card.add(about, BorderLayout.CENTER);
		return card;
	}

	private void configureQueueCard()
	{
		queueCard.setLayout(new BoxLayout(queueCard, BoxLayout.Y_AXIS));
		queueCard.setAlignmentX(LEFT_ALIGNMENT);

		JLabel queueHeading = new JLabel("Up next");
		queueHeading.setForeground(PRIMARY_TEXT);
		queueHeading.setFont(FontManager.getRunescapeBoldFont().deriveFont(17f));
		queueHeading.setAlignmentX(LEFT_ALIGNMENT);
		queueCard.add(queueHeading);
		queueCard.add(Box.createRigidArea(new Dimension(0, 3)));

		JLabel queueHint = new JLabel("Double-click  •  Drag to reorder");
		queueHint.setForeground(SECONDARY_TEXT);
		queueHint.setFont(FontManager.getRunescapeSmallFont().deriveFont(13f));
		queueHint.setAlignmentX(LEFT_ALIGNMENT);
		queueCard.add(queueHint);
		queueCard.add(Box.createRigidArea(new Dimension(0, 7)));

		emptyQueue.setForeground(SECONDARY_TEXT);
		emptyQueue.setFont(FontManager.getRunescapeSmallFont().deriveFont(14f));
		emptyQueue.setAlignmentX(LEFT_ALIGNMENT);
		emptyQueue.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
		queueCard.add(emptyQueue);

		queueList.setBackground(CARD_BACKGROUND);
		queueList.setForeground(PRIMARY_TEXT);
		queueList.setSelectionBackground(buttonHoverBackground());
		queueList.setSelectionForeground(PRIMARY_TEXT);
		queueList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		queueList.setFixedCellHeight(QUEUE_ITEM_HEIGHT);
		queueList.setCellRenderer(new QueueCellRenderer());
		queueList.setBorder(BorderFactory.createLineBorder(CARD_BORDER));
		queueList.setAlignmentX(LEFT_ALIGNMENT);
		queueList.setDropMode(DropMode.INSERT);
		queueList.setTransferHandler(new QueueTransferHandler());
		if (!GraphicsEnvironment.isHeadless())
		{
			queueList.setDragEnabled(true);
		}
		MouseAdapter queueMouseHandler = new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent event)
			{
				if (!queueList.isEnabled() || !SwingUtilities.isLeftMouseButton(event) || event.getClickCount() != 2)
				{
					return;
				}
				int index = queueList.locationToIndex(event.getPoint());
				Rectangle cell = index < 0 ? null : queueList.getCellBounds(index, index);
				if (cell != null && cell.contains(event.getPoint()))
				{
					plugin.playFromQueue(queueItems(), index);
				}
			}

			@Override
			public void mouseMoved(MouseEvent event)
			{
				int hoveredIndex = queueList.isEnabled() ? queueList.locationToIndex(event.getPoint()) : -1;
				Rectangle cell = hoveredIndex < 0 ? null : queueList.getCellBounds(hoveredIndex, hoveredIndex);
				setHoveredQueueIndex(cell != null && cell.contains(event.getPoint()) ? hoveredIndex : -1);
			}

			@Override
			public void mouseExited(MouseEvent event)
			{
				setHoveredQueueIndex(-1);
			}
		};
		queueList.addMouseListener(queueMouseHandler);
		queueList.addMouseMotionListener(queueMouseHandler);

		queueCard.add(queueList);
	}

	void stop()
	{
		progressTimer.stop();
		volumeCommitTimer.stop();
		JDialog dialog = aboutDialog;
		aboutDialog = null;
		if (dialog != null)
		{
			dialog.dispose();
		}
	}

	void setPlaybackState(SpotifyPlaybackState state)
	{
		boolean sameItem = playbackState.isItemAvailable() && state.isItemAvailable() &&
			playbackState.getTitle().equals(state.getTitle()) &&
			playbackState.getArtist().equals(state.getArtist());
		if (!sameItem || state.getDurationMs() <= 0)
		{
			sidebarSeeking = false;
			progress.setHovered(false);
		}
		playbackState = state;
		Font titleFont = FontManager.getRunescapeBoldFont().deriveFont(19f);
		Font artistFont = FontManager.getRunescapeFont().deriveFont(16f);
		title.setFont(UnicodeText.singleFontForText(titleFont, state.getTitle()));
		artist.setFont(UnicodeText.singleFontForText(artistFont, state.getArtist()));
		title.setText(state.getTitle());
		artist.setText(state.getArtist());
		playbackStatus.setText(state.isItemAvailable() ? (state.isPlaying() ? "Playing" : "Paused") : "Idle");
		playbackStatus.setForeground(state.isPlaying() ? sidebarAccent() : SECONDARY_TEXT);
		playPause.setIcon(state.isPlaying() ? PAUSE_TRANSPORT_ICON : PLAY_TRANSPORT_ICON);
		updatingVolume = true;
		if (state.isVolumeAvailable())
		{
			if (!volume.getValueIsAdjusting())
			{
				volume.setValue(state.getVolumePercent());
			}
			volumeValue.setText(volume.getValue() + "%");
		}
		else
		{
			volume.setValue(0);
			volumeValue.setText("Unavailable");
		}
		updatingVolume = false;
		updateProgress();
		updateButtons();
	}

	void setArtwork(BufferedImage image)
	{
		artwork.setImage(image);
	}

	void setQueueState(SpotifyQueueState state)
	{
		queueModel.clear();
		for (SpotifyQueueItem item : state.getItems())
		{
			if (queueModel.size() >= MAXIMUM_QUEUE_ITEMS)
			{
				break;
			}
			queueModel.addElement(item);
		}
		updateQueueSize();
		updateButtons();
	}

	void setConnected(boolean connected)
	{
		this.connected = connected;
		updateButtons();
	}

	void setSearchLoading(String query)
	{
		searchBusy = true;
		searchModel.clear();
		searchArtwork.clear();
		setSearchStatus("Searching for “" + query + "”…");
		updateButtons();
	}

	void setSearchResults(String query, List<SpotifySearchResult> results)
	{
		searchBusy = false;
		searchModel.clear();
		searchArtwork.clear();
		for (SpotifySearchResult result : results)
		{
			searchModel.addElement(result);
			plugin.loadSearchArtwork(result);
		}
		if (searchModel.isEmpty())
		{
			setSearchStatus("No songs found for “" + query + ".”");
		}
		else
		{
			setSearchStatus(searchModel.size() + (searchModel.size() == 1 ? " song found." : " songs found."));
			searchList.setSelectedIndex(0);
		}
		updateButtons();
	}

	void setSearchArtwork(String uri, String artworkUrl, BufferedImage image)
	{
		if (image == null)
		{
			return;
		}
		for (int index = 0; index < searchModel.size(); index++)
		{
			SpotifySearchResult result = searchModel.get(index);
			if (result.getUri().equals(uri) && result.getArtworkUrl().equals(artworkUrl))
			{
				searchArtwork.put(uri, image);
				searchList.repaint();
				return;
			}
		}
	}

	void setSearchError(String message)
	{
		searchBusy = false;
		setSearchStatus(message == null || message.trim().isEmpty()
			? "Spotify search failed."
			: message.trim());
		updateButtons();
	}

	void setSearchPlaybackStarted(SpotifySearchResult result)
	{
		searchBusy = false;
		setSearchStatus("Now playing “" + result.getTitle() + ".”");
		updateButtons();
	}

	private void setSearchStatus(String message)
	{
		searchStatus.setText(message);
		searchStatus.setToolTipText(message);
	}

	void setSpotifyApiStatus(String message)
	{
		String safeMessage = message == null || message.trim().isEmpty()
			? "Waiting for Spotify."
			: message.trim();
		String text = "Spotify API: " + safeMessage;
		spotifyApiStatus.setText(text);
		spotifyApiStatus.setToolTipText(text);
	}

	void setConfigurationState(boolean pluginEnabled, boolean hasClientId)
	{
		this.pluginEnabled = pluginEnabled;
		this.hasClientId = hasClientId;
		updateButtons();
	}

	void setSystemMediaMode(boolean systemMediaMode)
	{
		if (this.systemMediaMode != systemMediaMode)
		{
			volumeCommitTimer.stop();
		}
		this.systemMediaMode = systemMediaMode;
		sourceHeading.setText("Spotify");
		updateButtons();
	}

	void setOverlayVisible(boolean visible)
	{
		overlayVisible = visible;
		updateToggleAppearance(overlayToggle, overlayVisible, "In-game overlay");
	}

	void setOverlaySourceVisible(boolean visible)
	{
		overlaySourceVisible = visible;
		updateToggleAppearance(overlaySourceToggle, overlaySourceVisible, "Source");
	}

	void setOverlayTitleVisible(boolean visible)
	{
		overlayTitleVisible = visible;
		updateToggleAppearance(overlayTitleToggle, overlayTitleVisible, "Song title");
	}

	void setOverlayArtistVisible(boolean visible)
	{
		overlayArtistVisible = visible;
		updateToggleAppearance(overlayArtistToggle, overlayArtistVisible, "Artist");
	}

	void setOverlayControlsVisible(boolean visible)
	{
		overlayControlsVisible = visible;
		updateToggleAppearance(overlayControlsToggle, overlayControlsVisible, "Playback Controls");
	}

	void setOverlayProgressVisible(boolean visible)
	{
		overlayProgressVisible = visible;
		updateToggleAppearance(overlayProgressToggle, overlayProgressVisible, "Playback Progress");
	}

	void setOverlayArtworkVisible(boolean visible)
	{
		overlayArtworkVisible = visible;
		updateToggleAppearance(overlayArtworkToggle, overlayArtworkVisible, "Album Artwork");
	}

	void setOverlayLyricsVisible(boolean visible)
	{
		overlayLyricsVisible = visible;
		updateToggleAppearance(overlayLyricsToggle, overlayLyricsVisible, "Lyrics");
	}

	void setOverlayLyricsLines(int lines)
	{
		overlayLyricsLines = Math.max(1,
			Math.min(SpotifyControllerConfig.MAX_OVERLAY_LYRIC_LINES, lines));
		overlayLyricsLinesLabel.setText("Lyric lines: " + overlayLyricsLines);
		overlayLyricsDecrease.setEnabled(overlayLyricsLines > 1);
		overlayLyricsIncrease.setEnabled(
			overlayLyricsLines < SpotifyControllerConfig.MAX_OVERLAY_LYRIC_LINES);
	}

	private void updateButtons()
	{
		boolean canControl = playbackState.isItemAvailable() &&
			(systemMediaMode || (pluginEnabled && connected));
		previous.setEnabled(canControl);
		playPause.setEnabled(canControl);
		next.setEnabled(canControl);
		volume.setEnabled(canControl && playbackState.isVolumeAvailable());
		progress.setEnabled(canControl && playbackState.getDurationMs() > 0);
		if (!progress.isEnabled())
		{
			sidebarSeeking = false;
			progress.setHovered(false);
		}
		refresh.setEnabled(pluginEnabled && connected);
		disconnect.setEnabled(connected);
		connect.setEnabled(pluginEnabled && hasClientId && !connected);
		connect.setVisible(!connected);
		connectedControls.setVisible(connected);
		connectionCard.setVisible(pluginEnabled && hasClientId);
		queueCard.setVisible(!systemMediaMode && connected);
		queueList.setEnabled(!systemMediaMode && pluginEnabled && connected && !queueModel.isEmpty());
		boolean canUseSpotifySearch = pluginEnabled && hasClientId && !searchBusy;
		searchQuery.setEnabled(canUseSpotifySearch);
		searchButton.setEnabled(canUseSpotifySearch);
		searchList.setEnabled(canUseSpotifySearch && !searchModel.isEmpty());
		playSearchResult.setEnabled(canUseSpotifySearch && searchList.getSelectedValue() != null);
		updateSpotifyModeButtons();
		updateButtonAppearance(previous, false);
		updateButtonAppearance(playPause, false);
		updateButtonAppearance(next, false);
		updateButtonAppearance(refresh, false);
		updateButtonAppearance(disconnect, false);
		updateButtonAppearance(connect, true);
		updateButtonAppearance(searchButton, true);
		updateButtonAppearance(playSearchResult, true);
		revalidate();
		repaint();
	}

	private void updateSpotifyModeButtons()
	{
		boolean spotifyPlaying = shouldShowSpotifyModeControls(systemMediaMode, playbackState);
		boolean canControlSpotify = spotifyPlaying && pluginEnabled && connected;
		spotifyModeControls.setVisible(spotifyPlaying);
		shuffle.setEnabled(canControlSpotify);
		repeat.setEnabled(canControlSpotify);
		shuffle.putClientProperty("spotifyActive", playbackState.isShuffleEnabled());
		String repeatMode = playbackState.getRepeatMode();
		repeat.putClientProperty("spotifyActive", !"off".equals(repeatMode));
		repeat.putClientProperty("spotifyRepeatMode", repeatMode);
		shuffle.setToolTipText(playbackState.isShuffleEnabled()
			? "Shuffle: On (click to turn off)"
			: "Shuffle: Off (click to turn on)");
		if ("track".equals(repeatMode))
		{
			repeat.setToolTipText("Repeat: Current song (click to turn off)");
		}
		else if ("context".equals(repeatMode))
		{
			repeat.setToolTipText("Repeat: Playlist or album (click for current song)");
		}
		else
		{
			repeat.setToolTipText("Repeat: Off (click for playlist or album)");
		}
		updateSpotifyModeButtonAppearance(shuffle);
		updateSpotifyModeButtonAppearance(repeat);
	}

	static boolean shouldShowSpotifyModeControls(boolean systemMediaMode, SpotifyPlaybackState state)
	{
		return !systemMediaMode && state.isItemAvailable() && state.isPlaying();
	}

	private void updateProgress()
	{
		if (sidebarSeeking)
		{
			return;
		}
		int durationMs = playbackState.getDurationMs();
		int progressMs = Math.min(durationMs,
			playbackState.getEstimatedProgressMs(System.currentTimeMillis()));
		progress.setMaximum(Math.max(1, durationMs));
		progress.setValue(progressMs);
		elapsedTime.setText(formatTime(progressMs));
		remainingTime.setText("-" + formatTime(Math.max(0, durationMs - progressMs)));
	}

	private void updateSidebarSeekPreview(int mouseX)
	{
		int durationMs = playbackState.getDurationMs();
		int positionMs = seekPositionForX(
			mouseX - SeekBar.TRACK_PADDING,
			progress.getWidth() - (SeekBar.TRACK_PADDING * 2),
			durationMs);
		progress.setMaximum(Math.max(1, durationMs));
		progress.setValue(positionMs);
		elapsedTime.setText(formatTime(positionMs));
		remainingTime.setText("-" + formatTime(Math.max(0, durationMs - positionMs)));
	}

	static int seekPositionForX(int mouseX, int width, int durationMs)
	{
		if (width <= 0 || durationMs <= 0)
		{
			return 0;
		}
		double fraction = Math.max(0.0, Math.min(1.0, (double) mouseX / width));
		return (int) Math.round(durationMs * fraction);
	}

	private void updateQueueSize()
	{
		boolean empty = queueModel.isEmpty();
		emptyQueue.setVisible(empty);
		queueList.setVisible(!empty);
		int rows = Math.max(1, Math.min(MAXIMUM_QUEUE_ITEMS, queueModel.size()));
		int listHeight = rows * QUEUE_ITEM_HEIGHT + 2;
		queueList.setPreferredSize(new Dimension(1, listHeight));
		queueList.setMaximumSize(new Dimension(Integer.MAX_VALUE, listHeight));
		queueCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, empty ? 90 : 71 + listHeight));
		queueCard.revalidate();
		queueCard.repaint();
	}

	private void setHoveredQueueIndex(int index)
	{
		if (hoveredQueueIndex != index)
		{
			hoveredQueueIndex = index;
			queueList.repaint();
		}
	}

	private void showAboutDialog()
	{
		if (aboutDialog != null && aboutDialog.isDisplayable())
		{
			aboutDialog.toFront();
			aboutDialog.requestFocus();
			return;
		}

		Window owner = SwingUtilities.getWindowAncestor(this);
		JDialog dialog = new JDialog(owner, "About Spotify Controller", Dialog.ModalityType.MODELESS);
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		dialog.setResizable(true);

		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(PANEL_BACKGROUND);
		content.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

		JLabel heading = new JLabel("Spotify Controller");
		heading.setAlignmentX(Component.CENTER_ALIGNMENT);
		heading.setForeground(sidebarAccent());
		heading.setFont(FontManager.getRunescapeBoldFont().deriveFont(18f));
		content.add(heading);
		content.add(Box.createRigidArea(new Dimension(0, 13)));

		JLabel version = new JLabel("Plugin version: " + SpotifyControllerPlugin.PLUGIN_VERSION);
		version.setAlignmentX(Component.CENTER_ALIGNMENT);
		version.setForeground(PRIMARY_TEXT);
		version.setFont(FontManager.getRunescapeFont().deriveFont(13f));
		content.add(version);
		content.add(Box.createRigidArea(new Dimension(0, 6)));

		JLabel author = new JLabel("Made by iiDezi");
		author.setAlignmentX(Component.CENTER_ALIGNMENT);
		author.setForeground(SECONDARY_TEXT);
		author.setFont(FontManager.getRunescapeFont().deriveFont(13f));
		content.add(author);

		Dimension baseDialogSize = new Dimension(315, 150);
		Dimension initialDialogSize = new Dimension(390, 190);
		Runnable resizeAboutContents = () ->
		{
			double scale = Math.min(
				(double) dialog.getWidth() / baseDialogSize.width,
				(double) dialog.getHeight() / baseDialogSize.height);
			scale = Math.max(1.0, Math.min(3.0, scale));
			float fontScale = (float) scale;

			heading.setFont(FontManager.getRunescapeBoldFont().deriveFont(18f * fontScale));
			version.setFont(FontManager.getRunescapeFont().deriveFont(13f * fontScale));
			author.setFont(FontManager.getRunescapeFont().deriveFont(13f * fontScale));
			content.setBorder(BorderFactory.createEmptyBorder(
				(int) Math.round(18 * scale),
				(int) Math.round(20 * scale),
				(int) Math.round(18 * scale),
				(int) Math.round(20 * scale)));
			content.revalidate();
			content.repaint();
		};
		dialog.addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentResized(ComponentEvent event)
			{
				resizeAboutContents.run();
			}
		});

		dialog.setContentPane(content);
		dialog.pack();
		dialog.setMinimumSize(baseDialogSize);
		dialog.setSize(initialDialogSize);
		resizeAboutContents.run();
		dialog.setLocationRelativeTo(owner);
		aboutDialog = dialog;
		dialog.setVisible(true);
	}

	private List<SpotifyQueueItem> queueItems()
	{
		List<SpotifyQueueItem> items = new ArrayList<>();
		for (int index = 0; index < queueModel.size(); index++)
		{
			items.add(queueModel.get(index));
		}
		return items;
	}

	private void configureSpotifyModeButton(JButton button)
	{
		button.setText(null);
		button.setFocusPainted(false);
		button.setRolloverEnabled(true);
		button.setFocusable(false);
		button.setMargin(new Insets(0, 0, 0, 0));
		button.setOpaque(true);
		button.setContentAreaFilled(true);
		button.setPreferredSize(new Dimension(32, 31));
		button.setMaximumSize(new Dimension(32, 31));
		button.addChangeListener(event -> updateSpotifyModeButtonAppearance(button));
		updateSpotifyModeButtonAppearance(button);
	}

	private void updateSpotifyModeButtonAppearance(JButton button)
	{
		boolean enabled = button.isEnabled();
		boolean active = Boolean.TRUE.equals(button.getClientProperty("spotifyActive"));
		boolean hovered = enabled && button.getModel().isRollover();
		button.setBackground(enabled
			? (hovered ? buttonHoverBackground() : BUTTON_BACKGROUND)
			: BUTTON_DISABLED);
		button.setBorder(BorderFactory.createLineBorder(enabled && active
			? sidebarAccent()
			: (enabled ? CARD_BORDER : new Color(48, 48, 48))));
		button.repaint();
	}

	private static void configureToggleButton(JButton button)
	{
		button.setFont(FontManager.getRunescapeBoldFont().deriveFont(16f));
		button.setFocusPainted(false);
		button.setRolloverEnabled(true);
		button.setMargin(new Insets(2, 4, 2, 4));
		button.setOpaque(true);
		button.setContentAreaFilled(true);
		button.setAlignmentX(LEFT_ALIGNMENT);
		button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
	}

	private void configureSidebarTab(JButton button)
	{
		button.setFont(FontManager.getRunescapeBoldFont().deriveFont(14f));
		button.setFocusPainted(false);
		button.setRolloverEnabled(true);
		button.setMargin(new Insets(3, 4, 3, 4));
		button.setOpaque(true);
		button.setContentAreaFilled(true);
		button.setPreferredSize(new Dimension(1, 32));
		button.addChangeListener(event -> updateSidebarTabAppearance());
	}

	private void showSidebarView(String view)
	{
		selectedSidebarView = SEARCH_VIEW.equals(view) || SETTINGS_VIEW.equals(view)
			? view
			: PLAYER_VIEW;
		sidebarCardLayout.show(sidebarCards, selectedSidebarView);
		updateSidebarTabAppearance();
	}

	private void updateSidebarTabAppearance()
	{
		updateSidebarTabButton(playerTab, PLAYER_VIEW.equals(selectedSidebarView));
		updateSidebarTabButton(searchTab, SEARCH_VIEW.equals(selectedSidebarView));
		updateSidebarTabButton(settingsTab, SETTINGS_VIEW.equals(selectedSidebarView));
	}

	private void updateSidebarTabButton(JButton button, boolean selected)
	{
		boolean hovered = button.getModel().isRollover();
		button.setBackground(selected
			? selectedBackground(hovered)
			: (hovered ? buttonHoverBackground() : BUTTON_BACKGROUND));
		button.setForeground(selected || hovered ? sidebarAccent() : PRIMARY_TEXT);
		button.setBorder(BorderFactory.createLineBorder(selected || hovered ? sidebarAccent() : CARD_BORDER));
	}

	private void updateToggleAppearance(JButton button, boolean selected, String label)
	{
		boolean hovered = button.getModel().isRollover();
		button.setText(label + ": " + (selected ? "ON" : "OFF"));
		button.setForeground(selected ? sidebarAccentHover() : OVERLAY_OFF_TEXT);
		button.setBackground(selected
			? selectedBackground(hovered)
			: (hovered ? OVERLAY_OFF_HOVER : OVERLAY_OFF_BACKGROUND));
		button.setBorder(BorderFactory.createLineBorder(selected ? sidebarAccent() : OVERLAY_OFF_BORDER));
	}

	private void styleDarkButton(AbstractButton button)
	{
		styleButton(button, false);
	}

	private void styleAccentButton(AbstractButton button)
	{
		styleButton(button, true);
	}

	private void styleButton(AbstractButton button, boolean accent)
	{
		button.setFocusPainted(false);
		button.setRolloverEnabled(true);
		button.setMargin(new Insets(2, 4, 2, 4));
		button.setOpaque(true);
		button.setContentAreaFilled(true);
		button.addChangeListener(event -> updateButtonAppearance(button, accent));
		updateButtonAppearance(button, accent);
	}

	private void updateButtonAppearance(AbstractButton button, boolean accent)
	{
		boolean enabled = button.isEnabled();
		boolean hovered = enabled && button.getModel().isRollover();
		if (!enabled)
		{
			button.setBackground(BUTTON_DISABLED);
			button.setForeground(new Color(105, 105, 105));
			button.setBorder(BorderFactory.createLineBorder(new Color(48, 48, 48)));
			return;
		}

		if (accent)
		{
			Color accentColor = hovered ? sidebarAccentHover() : sidebarAccent();
			button.setBackground(accentColor);
			button.setForeground(contrastingText(accentColor));
			button.setBorder(BorderFactory.createLineBorder(sidebarAccent()));
		}
		else
		{
			button.setBackground(hovered ? buttonHoverBackground() : BUTTON_BACKGROUND);
			button.setForeground(hovered ? sidebarAccent() : PRIMARY_TEXT);
			button.setBorder(BorderFactory.createLineBorder(hovered ? sidebarAccent() : CARD_BORDER));
		}
	}

	void refreshTheme()
	{
		sourceHeading.setForeground(sidebarAccent());
		playbackStatus.setForeground(playbackState.isPlaying() ? sidebarAccent() : SECONDARY_TEXT);
		queueList.setSelectionBackground(buttonHoverBackground());
		searchList.setSelectionBackground(buttonHoverBackground());
		overlayAccentButton.setText("In-game accent: " + toHex(overlayAccent()));
		sidebarAccentButton.setText("Sidebar accent: " + toHex(sidebarAccent()));
		overlayProgressTrackButton.setText("Progress track: " + toHex(overlayProgressTrack()));
		overlayPlaybackProgressButton.setText("Playback progress: " + toHex(overlayPlaybackProgress()));
		overlayBackgroundButton.setText("Overlay background: " + toHex(overlayBackground()));
		overlayPlaybackButtonColorButton.setText(
			"Playback button icons: " + toHex(overlayPlaybackButtonColor()));
		updateSidebarTabAppearance();
		updateToggleAppearance(overlayToggle, overlayVisible, "In-game overlay");
		updateToggleAppearance(overlaySourceToggle, overlaySourceVisible, "Source");
		updateToggleAppearance(overlayTitleToggle, overlayTitleVisible, "Song title");
		updateToggleAppearance(overlayArtistToggle, overlayArtistVisible, "Artist");
		updateToggleAppearance(overlayControlsToggle, overlayControlsVisible, "Playback Controls");
		updateToggleAppearance(overlayProgressToggle, overlayProgressVisible, "Playback Progress");
		updateToggleAppearance(overlayArtworkToggle, overlayArtworkVisible, "Album Artwork");
		updateToggleAppearance(overlayLyricsToggle, overlayLyricsVisible, "Lyrics");
		updateSpotifyModeButtonAppearance(shuffle);
		updateSpotifyModeButtonAppearance(repeat);
		updateButtonAppearance(previous, false);
		updateButtonAppearance(playPause, false);
		updateButtonAppearance(next, false);
		updateButtonAppearance(connect, true);
		updateButtonAppearance(disconnect, false);
		updateButtonAppearance(refresh, false);
		updateButtonAppearance(searchButton, true);
		updateButtonAppearance(playSearchResult, true);
		updateButtonAppearance(about, false);
		updateButtonAppearance(overlayAccentButton, false);
		updateButtonAppearance(sidebarAccentButton, false);
		updateButtonAppearance(overlayProgressTrackButton, false);
		updateButtonAppearance(overlayPlaybackProgressButton, false);
		updateButtonAppearance(overlayBackgroundButton, false);
		updateButtonAppearance(overlayPlaybackButtonColorButton, false);
		progress.repaint();
		volume.repaint();
		queueList.repaint();
		searchList.repaint();
		repaint();
	}

	private Color overlayAccent()
	{
		return normalizeAccent(config.overlayAccentColor());
	}

	private Color sidebarAccent()
	{
		return normalizeAccent(config.sidebarAccentColor());
	}

	private Color overlayProgressTrack()
	{
		return normalizeColor(config.overlayProgressTrackColor(), DEFAULT_OVERLAY_PROGRESS_TRACK);
	}

	private Color overlayPlaybackProgress()
	{
		return normalizeColor(config.overlayPlaybackProgressColor(), DEFAULT_OVERLAY_PLAYBACK_PROGRESS);
	}

	private Color overlayBackground()
	{
		Color color = config.overlayBackgroundColor();
		return color == null ? DEFAULT_OVERLAY_BACKGROUND : color;
	}

	private Color overlayPlaybackButtonColor()
	{
		return normalizeColor(config.overlayPlaybackButtonColor(), DEFAULT_OVERLAY_PLAYBACK_BUTTON);
	}

	private Color sidebarAccentHover()
	{
		return blend(sidebarAccent(), Color.WHITE, 0.12);
	}

	private Color buttonHoverBackground()
	{
		return blend(sidebarAccent(), Color.BLACK, 0.68);
	}

	private Color selectedBackground(boolean hovered)
	{
		return blend(sidebarAccent(), Color.BLACK, hovered ? 0.56 : 0.67);
	}

	private static Color normalizeAccent(Color color)
	{
		return normalizeColor(color, DEFAULT_ACCENT);
	}

	private static Color normalizeColor(Color color, Color fallback)
	{
		Color selected = color == null ? fallback : color;
		return new Color(selected.getRed(), selected.getGreen(), selected.getBlue());
	}

	private static Color blend(Color first, Color second, double secondWeight)
	{
		double weight = Math.max(0.0, Math.min(1.0, secondWeight));
		int red = (int) Math.round(first.getRed() * (1.0 - weight) + second.getRed() * weight);
		int green = (int) Math.round(first.getGreen() * (1.0 - weight) + second.getGreen() * weight);
		int blue = (int) Math.round(first.getBlue() * (1.0 - weight) + second.getBlue() * weight);
		return new Color(red, green, blue);
	}

	private static Color contrastingText(Color background)
	{
		double luminance = (0.2126 * background.getRed()) +
			(0.7152 * background.getGreen()) + (0.0722 * background.getBlue());
		return luminance >= 145.0 ? Color.BLACK : Color.WHITE;
	}

	private static String toHex(Color color)
	{
		return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
	}

	private static String shortenToWidth(
		String value,
		Graphics2D graphics,
		Font font,
		int maximumWidth)
	{
		if (value == null || value.isEmpty() || UnicodeText.stringWidth(graphics, font, value) <= maximumWidth)
		{
			return value == null ? "" : value;
		}
		String ellipsis = "…";
		int codePoints = value.codePointCount(0, value.length());
		while (codePoints > 0)
		{
			int end = value.offsetByCodePoints(0, codePoints);
			String candidate = value.substring(0, end) + ellipsis;
			if (UnicodeText.stringWidth(graphics, font, candidate) <= maximumWidth)
			{
				return candidate;
			}
			codePoints--;
		}
		return ellipsis;
	}

	private static String formatTime(int milliseconds)
	{
		int seconds = Math.max(0, milliseconds / 1000);
		int hours = seconds / 3600;
		int minutes = (seconds % 3600) / 60;
		int remainingSeconds = seconds % 60;
		return hours > 0
			? hours + ":" + String.format("%02d:%02d", minutes, remainingSeconds)
			: minutes + ":" + String.format("%02d", remainingSeconds);
	}

	private static JTextArea textArea(Font font)
	{
		JTextArea area = new JTextArea();
		area.setEditable(false);
		area.setFocusable(false);
		area.setOpaque(false);
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		area.setFont(font);
		area.setForeground(PRIMARY_TEXT);
		area.setBorder(null);
		return area;
	}

	private static JPanel transparentPanel()
	{
		return transparentPanel(null);
	}

	private static JPanel transparentPanel(LayoutManager layout)
	{
		JPanel panel = layout == null ? new JPanel() : new JPanel(layout);
		panel.setOpaque(false);
		return panel;
	}

	private static void addFullWidth(JPanel parent, JTextArea area, int maximumHeight)
	{
		area.setAlignmentX(LEFT_ALIGNMENT);
		area.setMaximumSize(new Dimension(Integer.MAX_VALUE, maximumHeight));
		parent.add(area);
	}

	private final class QueueTransferHandler extends TransferHandler
	{
		private static final long serialVersionUID = 1L;
		private int sourceIndex = -1;

		@Override
		protected Transferable createTransferable(JComponent component)
		{
			sourceIndex = queueList.getSelectedIndex();
			return new StringSelection(Integer.toString(sourceIndex));
		}

		@Override
		public int getSourceActions(JComponent component)
		{
			return MOVE;
		}

		@Override
		public boolean canImport(TransferSupport support)
		{
			return support.isDrop() && queueList.isEnabled() &&
				support.isDataFlavorSupported(DataFlavor.stringFlavor);
		}

		@Override
		public boolean importData(TransferSupport support)
		{
			if (!canImport(support) || sourceIndex < 0 || sourceIndex >= queueModel.size())
			{
				return false;
			}

			JList.DropLocation location = (JList.DropLocation) support.getDropLocation();
			int targetIndex = location.getIndex();
			if (targetIndex > sourceIndex)
			{
				targetIndex--;
			}
			targetIndex = Math.max(0, Math.min(queueModel.size() - 1, targetIndex));
			if (targetIndex == sourceIndex)
			{
				return false;
			}

			SpotifyQueueItem item = queueModel.remove(sourceIndex);
			queueModel.add(targetIndex, item);
			queueList.setSelectedIndex(targetIndex);
			plugin.reorderQueue(queueItems());
			return true;
		}
	}

	private final class QueueCellRenderer extends JComponent implements ListCellRenderer<SpotifyQueueItem>
	{
		private static final long serialVersionUID = 1L;
		private SpotifyQueueItem item;
		private int index;
		private boolean selected;

		private QueueCellRenderer()
		{
			setOpaque(true);
		}

		@Override
		public Component getListCellRendererComponent(
			JList<? extends SpotifyQueueItem> list,
			SpotifyQueueItem value,
			int index,
			boolean selected,
			boolean focused)
		{
			this.item = value;
			this.index = index;
			this.selected = selected;
			return this;
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D graphics2D = (Graphics2D) graphics.create();
			try
			{
				graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
					RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				boolean highlighted = selected || index == hoveredQueueIndex;
				graphics2D.setColor(highlighted ? buttonHoverBackground() : CARD_BACKGROUND);
				graphics2D.fillRect(0, 0, getWidth(), getHeight());
				graphics2D.setColor(new Color(42, 42, 42));
				graphics2D.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
				if (item == null)
				{
					return;
				}

				graphics2D.setFont(FontManager.getRunescapeSmallFont().deriveFont(13f));
				graphics2D.setColor(highlighted ? sidebarAccent() : SECONDARY_TEXT);
				graphics2D.drawString(Integer.toString(index + 1), 7, 29);

				int textX = 29;
				int textWidth = Math.max(20, getWidth() - textX - 22);
				Font titleFont = FontManager.getRunescapeBoldFont().deriveFont(14f);
				graphics2D.setFont(titleFont);
				graphics2D.setColor(PRIMARY_TEXT);
				UnicodeText.drawString(graphics2D, titleFont,
					shortenToWidth(item.getTitle(), graphics2D, titleFont, textWidth), textX, 19);
				Font artistFont = FontManager.getRunescapeSmallFont().deriveFont(15f);
				graphics2D.setFont(artistFont);
				graphics2D.setColor(SECONDARY_TEXT);
				UnicodeText.drawString(graphics2D, artistFont,
					shortenToWidth(item.getArtist(), graphics2D, artistFont, textWidth), textX, 41);

				graphics2D.setFont(FontManager.getRunescapeBoldFont().deriveFont(16f));
				graphics2D.drawString("≡", getWidth() - 16, 29);
			}
			finally
			{
				graphics2D.dispose();
			}
		}
	}

	private final class SearchResultRenderer extends JComponent implements ListCellRenderer<SpotifySearchResult>
	{
		private static final long serialVersionUID = 1L;
		private SpotifySearchResult result;
		private boolean selected;

		private SearchResultRenderer()
		{
			setOpaque(true);
		}

		@Override
		public Component getListCellRendererComponent(
			JList<? extends SpotifySearchResult> list,
			SpotifySearchResult value,
			int index,
			boolean selected,
			boolean focused)
		{
			this.result = value;
			this.selected = selected;
			return this;
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D graphics2D = (Graphics2D) graphics.create();
			try
			{
				graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
					RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
					RenderingHints.VALUE_INTERPOLATION_BICUBIC);
				graphics2D.setColor(selected ? buttonHoverBackground() : CARD_BACKGROUND);
				graphics2D.fillRect(0, 0, getWidth(), getHeight());
				graphics2D.setColor(new Color(42, 42, 42));
				graphics2D.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
				if (result == null)
				{
					return;
				}

				int artworkX = 6;
				int artworkY = (getHeight() - SEARCH_ARTWORK_SIZE) / 2;
				BufferedImage image = searchArtwork.get(result.getUri());
				if (image == null)
				{
					graphics2D.setColor(BUTTON_BACKGROUND);
					graphics2D.fillRoundRect(
						artworkX, artworkY, SEARCH_ARTWORK_SIZE, SEARCH_ARTWORK_SIZE, 5, 5);
					graphics2D.setColor(selected ? sidebarAccent() : SECONDARY_TEXT);
					graphics2D.setFont(FontManager.getRunescapeBoldFont().deriveFont(18f));
					String placeholder = "♫";
					FontMetrics metrics = graphics2D.getFontMetrics();
					graphics2D.drawString(
						placeholder,
						artworkX + (SEARCH_ARTWORK_SIZE - metrics.stringWidth(placeholder)) / 2,
						artworkY + (SEARCH_ARTWORK_SIZE - metrics.getHeight()) / 2 + metrics.getAscent());
				}
				else
				{
					graphics2D.drawImage(image,
						artworkX,
						artworkY,
						SEARCH_ARTWORK_SIZE,
						SEARCH_ARTWORK_SIZE,
						null);
				}
				graphics2D.setColor(selected ? sidebarAccent() : CARD_BORDER);
				graphics2D.drawRoundRect(
					artworkX, artworkY, SEARCH_ARTWORK_SIZE - 1, SEARCH_ARTWORK_SIZE - 1, 5, 5);

				int textX = artworkX + SEARCH_ARTWORK_SIZE + 8;
				int textWidth = Math.max(20, getWidth() - textX - 7);
				Font titleFont = FontManager.getRunescapeBoldFont().deriveFont(14f);
				graphics2D.setFont(titleFont);
				graphics2D.setColor(PRIMARY_TEXT);
				UnicodeText.drawString(graphics2D, titleFont,
					shortenToWidth(result.getTitle(), graphics2D, titleFont, textWidth),
					textX,
					21);

				String detail = result.getArtist();
				if (!result.getAlbum().isEmpty())
				{
					detail += " • " + result.getAlbum();
				}
				Font detailFont = FontManager.getRunescapeSmallFont().deriveFont(14f);
				graphics2D.setFont(detailFont);
				graphics2D.setColor(SECONDARY_TEXT);
				UnicodeText.drawString(graphics2D, detailFont,
					shortenToWidth(detail, graphics2D, detailFont, textWidth),
					textX,
					43);
			}
			finally
			{
				graphics2D.dispose();
			}
		}
	}

	private final class SeekBar extends JComponent
	{
		private static final long serialVersionUID = 1L;
		private static final int TRACK_HEIGHT = 4;
		private static final int THUMB_SIZE = 12;
		private static final int TRACK_PADDING = THUMB_SIZE / 2;
		private int maximum = 1;
		private int value;
		private boolean hovered;

		private void setMaximum(int maximum)
		{
			this.maximum = Math.max(1, maximum);
			value = Math.min(value, this.maximum);
			repaint();
		}

		private void setValue(int value)
		{
			this.value = Math.max(0, Math.min(maximum, value));
			repaint();
		}

		private int getValue()
		{
			return value;
		}

		private void setHovered(boolean hovered)
		{
			if (this.hovered != hovered)
			{
				this.hovered = hovered;
				repaint();
			}
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D graphics2D = (Graphics2D) graphics.create();
			try
			{
				graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
				int trackWidth = Math.max(1, getWidth() - (TRACK_PADDING * 2));
				int trackY = (getHeight() - TRACK_HEIGHT) / 2;
				double fraction = maximum <= 0 ? 0.0 : (double) value / maximum;
				int filledWidth = (int) Math.round(trackWidth * fraction);

				graphics2D.setColor(PROGRESS_TRACK);
				graphics2D.fillRoundRect(TRACK_PADDING, trackY, trackWidth, TRACK_HEIGHT,
					TRACK_HEIGHT, TRACK_HEIGHT);
				if (filledWidth > 0)
				{
					graphics2D.setColor(isEnabled() ? sidebarAccent() : new Color(75, 91, 81));
					graphics2D.fillRoundRect(TRACK_PADDING, trackY, filledWidth, TRACK_HEIGHT,
						TRACK_HEIGHT, TRACK_HEIGHT);
				}

				if (isEnabled())
				{
					int thumbCenterX = TRACK_PADDING + filledWidth;
					graphics2D.setColor(hovered ? sidebarAccentHover() : PRIMARY_TEXT);
					graphics2D.fillOval(
						thumbCenterX - (THUMB_SIZE / 2),
						(getHeight() - THUMB_SIZE) / 2,
						THUMB_SIZE,
						THUMB_SIZE);
				}
			}
			finally
			{
				graphics2D.dispose();
			}
		}
	}

	private static final class TransportIcon implements Icon
	{
		private static final int PREVIOUS = 0;
		private static final int PLAY = 1;
		private static final int PAUSE = 2;
		private static final int NEXT = 3;
		private static final int ICON_WIDTH = 22;
		private static final int ICON_HEIGHT = 14;
		private final int type;

		private TransportIcon(int type)
		{
			this.type = type;
		}

		@Override
		public int getIconWidth()
		{
			return ICON_WIDTH;
		}

		@Override
		public int getIconHeight()
		{
			return ICON_HEIGHT;
		}

		@Override
		public void paintIcon(Component component, Graphics graphics, int x, int y)
		{
			Graphics2D graphics2D = (Graphics2D) graphics.create();
			try
			{
				graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
				graphics2D.setColor(component.isEnabled() ? PRIMARY_TEXT : new Color(105, 105, 105));
				if (type == PREVIOUS)
				{
					graphics2D.fillRect(x + 3, y + 2, 2, 10);
					graphics2D.fillPolygon(
						new int[]{x + 17, x + 7, x + 17},
						new int[]{y + 2, y + 7, y + 12},
						3);
				}
				else if (type == NEXT)
				{
					graphics2D.fillPolygon(
						new int[]{x + 5, x + 15, x + 5},
						new int[]{y + 2, y + 7, y + 12},
						3);
					graphics2D.fillRect(x + 17, y + 2, 2, 10);
				}
				else if (type == PAUSE)
				{
					graphics2D.fillRoundRect(x + 6, y + 2, 3, 10, 1, 1);
					graphics2D.fillRoundRect(x + 13, y + 2, 3, 10, 1, 1);
				}
				else
				{
					graphics2D.fillPolygon(
						new int[]{x + 7, x + 17, x + 7},
						new int[]{y + 2, y + 7, y + 12},
						3);
				}
			}
			finally
			{
				graphics2D.dispose();
			}
		}
	}

	private final class PlaybackModeIcon implements Icon
	{
		private static final int ICON_SIZE = 19;
		private final boolean shuffleIcon;

		private PlaybackModeIcon(boolean shuffleIcon)
		{
			this.shuffleIcon = shuffleIcon;
		}

		@Override
		public int getIconWidth()
		{
			return ICON_SIZE;
		}

		@Override
		public int getIconHeight()
		{
			return ICON_SIZE;
		}

		@Override
		public void paintIcon(Component component, Graphics graphics, int x, int y)
		{
			Graphics2D graphics2D = (Graphics2D) graphics.create();
			try
			{
				graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
				boolean enabled = component.isEnabled();
				boolean active = component instanceof JComponent &&
					Boolean.TRUE.equals(((JComponent) component).getClientProperty("spotifyActive"));
				graphics2D.setColor(enabled
					? (active ? sidebarAccentHover() :
						(shuffleIcon ? new Color(125, 125, 125) : PRIMARY_TEXT))
					: new Color(105, 105, 105));
				graphics2D.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				if (shuffleIcon)
				{
					paintShuffle(graphics2D, x, y);
				}
				else
				{
					paintRepeat(graphics2D, component, x, y);
				}
			}
			finally
			{
				graphics2D.dispose();
			}
		}

		private void paintShuffle(Graphics2D graphics, int x, int y)
		{
			int left = x + 1;
			int right = x + 17;
			int top = y + 4;
			int bottom = y + 15;
			graphics.drawLine(left, top, left + 4, top);
			graphics.drawLine(left + 4, top, right - 3, bottom);
			graphics.drawLine(right - 3, bottom, right, bottom);
			graphics.drawLine(right, bottom, right - 3, bottom - 3);
			graphics.drawLine(right, bottom, right - 3, bottom + 3);

			graphics.drawLine(left, bottom, left + 4, bottom);
			graphics.drawLine(left + 4, bottom, right - 3, top);
			graphics.drawLine(right - 3, top, right, top);
			graphics.drawLine(right, top, right - 3, top - 3);
			graphics.drawLine(right, top, right - 3, top + 3);
		}

		private void paintRepeat(Graphics2D graphics, Component component, int x, int y)
		{
			int left = x + 2;
			int right = x + 16;
			int top = y + 5;
			int bottom = y + 14;
			graphics.drawLine(left + 2, top, right, top);
			graphics.drawLine(right, top, right - 3, top - 3);
			graphics.drawLine(right, top, right - 3, top + 3);
			graphics.drawLine(right, top + 2, right, bottom - 2);
			graphics.drawLine(right - 2, bottom, left, bottom);
			graphics.drawLine(left, bottom, left + 3, bottom - 3);
			graphics.drawLine(left, bottom, left + 3, bottom + 3);
			graphics.drawLine(left, bottom - 2, left, top + 2);

			if (component instanceof JComponent &&
				"track".equals(((JComponent) component).getClientProperty("spotifyRepeatMode")))
			{
				graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 8));
				graphics.drawString("1", x + 8, y + 12);
			}
		}
	}

	private final class VolumeSliderUI extends BasicSliderUI
	{
		private static final int TRACK_HEIGHT = 4;

		private VolumeSliderUI(JSlider slider)
		{
			super(slider);
		}

		@Override
		protected Dimension getThumbSize()
		{
			return new Dimension(12, 12);
		}

		@Override
		public void paintTrack(Graphics graphics)
		{
			Graphics2D graphics2D = (Graphics2D) graphics.create();
			try
			{
				graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
				int y = trackRect.y + ((trackRect.height - TRACK_HEIGHT) / 2);
				graphics2D.setColor(PROGRESS_TRACK);
				graphics2D.fillRoundRect(trackRect.x, y, trackRect.width, TRACK_HEIGHT,
					TRACK_HEIGHT, TRACK_HEIGHT);
				if (slider.isEnabled())
				{
					int filled = Math.max(0, xPositionForValue(slider.getValue()) - trackRect.x);
					graphics2D.setColor(sidebarAccent());
					graphics2D.fillRoundRect(trackRect.x, y, filled, TRACK_HEIGHT,
						TRACK_HEIGHT, TRACK_HEIGHT);
				}
			}
			finally
			{
				graphics2D.dispose();
			}
		}

		@Override
		public void paintThumb(Graphics graphics)
		{
			Graphics2D graphics2D = (Graphics2D) graphics.create();
			try
			{
				graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
				graphics2D.setColor(slider.isEnabled() ? PRIMARY_TEXT : new Color(92, 92, 92));
				graphics2D.fillOval(thumbRect.x, thumbRect.y, thumbRect.width, thumbRect.height);
				if (slider.isEnabled())
				{
					graphics2D.setColor(sidebarAccent());
					graphics2D.drawOval(thumbRect.x, thumbRect.y, thumbRect.width - 1, thumbRect.height - 1);
				}
			}
			finally
			{
				graphics2D.dispose();
			}
		}
	}

	private static final class Artwork extends JComponent
	{
		private static final long serialVersionUID = 1L;
		private BufferedImage image;

		private Artwork()
		{
			Dimension size = new Dimension(ARTWORK_SIZE, ARTWORK_SIZE);
			setPreferredSize(size);
			setMinimumSize(size);
			setMaximumSize(size);
		}

		private void setImage(BufferedImage image)
		{
			this.image = image;
			repaint();
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D graphics2D = (Graphics2D) graphics.create();
			try
			{
				graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
				graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
					RenderingHints.VALUE_INTERPOLATION_BICUBIC);
				graphics2D.setColor(BUTTON_BACKGROUND);
				graphics2D.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
				if (image == null)
				{
					graphics2D.setColor(SECONDARY_TEXT);
					graphics2D.setFont(FontManager.getRunescapeBoldFont().deriveFont(30f));
					String placeholder = "♫";
					FontMetrics metrics = graphics2D.getFontMetrics();
					int x = (getWidth() - metrics.stringWidth(placeholder)) / 2;
					int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
					graphics2D.drawString(placeholder, x, y);
					return;
				}

				double scale = Math.min(
					(double) getWidth() / image.getWidth(),
					(double) getHeight() / image.getHeight());
				int width = Math.max(1, (int) Math.round(image.getWidth() * scale));
				int height = Math.max(1, (int) Math.round(image.getHeight() * scale));
				int x = (getWidth() - width) / 2;
				int y = (getHeight() - height) / 2;
				graphics2D.drawImage(image, x, y, width, height, null);
			}
			finally
			{
				graphics2D.dispose();
			}
		}
	}

	private static final class RoundedPanel extends JPanel
	{
		private static final long serialVersionUID = 1L;

		private RoundedPanel()
		{
			setOpaque(false);
			setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D graphics2D = (Graphics2D) graphics.create();
			try
			{
				graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
				graphics2D.setColor(CARD_BACKGROUND);
				graphics2D.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 11, 11);
				graphics2D.setColor(CARD_BORDER);
				graphics2D.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 11, 11);
			}
			finally
			{
				graphics2D.dispose();
			}
			super.paintComponent(graphics);
		}
	}
}
