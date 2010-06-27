package mindnotes.client.ui;

import mindnotes.client.model.Node;
import mindnotes.client.presentation.MindMapView;
import mindnotes.client.presentation.NodeView;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.ScrollPanel;

public class MindMapWidget extends Composite implements MindMapView,
		NodeContainer, RequiresResize {

	private AbsolutePanel _viewportPanel;

	private ScrollPanel _scrollPanel;
	private ArrowsWidget _arrowsWidget;
	private ActionsPanel _actionsPanel;
	private Listener _listener;
	private NodeWidget _actionsPanelView;

	private NodeWidget _rootNode;

	private NodeLayout _layout;

	public MindMapWidget() {
		_layout = new NodeLayout();
		_actionsPanel = new ActionsPanel(this);
		_actionsPanel.setVisible(false);

		_actionsPanel.setListener(new ActionsPanel.Listener() {

			@Override
			public void deleteClicked() {
				_listener.deleteGesture();
			}

			@Override
			public void addRightClicked() {
				_listener.addRightGesture();
			}

			@Override
			public void addLeftClicked() {
				_listener.addLeftGesture();
			}

			@Override
			public void addClicked() {
				_listener.addGesture();
			}
		});

		_arrowsWidget = new ArrowsWidget(this);
		_arrowsWidget.setPixelSize(1000, 1000);

		_rootNode = new NodeWidget();
		_rootNode.setArrowMaker(_arrowsWidget);
		_rootNode.setContainer(this);

		_viewportPanel = new AbsolutePanel();
		_viewportPanel.add(_arrowsWidget, 0, 0);
		_viewportPanel.add(_rootNode, 500, 500);
		_viewportPanel.add(_actionsPanel, 0, 0);

		_scrollPanel = new ScrollPanel(_viewportPanel);
		// _scrollPanel.setPixelSize(600, 500);
		_scrollPanel.setScrollPosition(250);
		_scrollPanel.setHorizontalScrollPosition(250);
		_scrollPanel.setAlwaysShowScrollBars(true);
		_viewportPanel.setPixelSize(1000, 1000);
		_viewportPanel.addStyleName("checkers-bg");
		initWidget(_scrollPanel);

	}

	@Override
	public NodeView getRootNodeView() {
		return _rootNode;
	}

	/**
	 * Push a redraw request to the underlying canvas widgets.
	 */
	public void redraw() {
		_arrowsWidget.render();
	}

	public int getNodeRelativeTop(NodeWidget node) {
		return node.getBubbleTop() - _viewportPanel.getAbsoluteTop();
	}

	public int getNodeRelativeLeft(NodeWidget node) {
		return node.getBubbleLeft() - _viewportPanel.getAbsoluteLeft();
	}

	public AbsolutePanel getViewportPanel() {
		return _viewportPanel;
	}

	@Override
	public void showActionsPanel(NodeView view, Node node) {
		// TODO maybe move this to ActionsPanel
		_actionsPanelView = (NodeWidget) view;
		_actionsPanel.showNextTo((NodeWidget) view);
	}

	@Override
	public void setListener(Listener l) {
		_listener = l;
	}

	@Override
	public void hideActionsPanel() {
		_actionsPanelView = null;
		_actionsPanel.setVisible(false);
	}

	@Override
	public void onResize() {

		updateLayout();
	}

	/**
	 * Also called by arrowmaker
	 */
	public void onClick() {
		// let the presenter decide what to do
		if (_listener != null)
			_listener.clickGesture();
	}

	@Override
	public void removeNode(NodeWidget node) {
		_viewportPanel.remove(node);
		for (NodeWidget child : node.getChildren()) {
			removeNode(child);
		}
	}

	@Override
	public void addNode(NodeWidget node) {
		_viewportPanel.add(node);
	}

	public void updateLayout() {
		_layout.doLayout(_rootNode);

		// use top level node's suggested width and height as a suggested size
		// for the whole tree.
		Box bounds = _rootNode.getBranchBounds();

		// suggest a new size for the viewport panel
		int paneWidth = _scrollPanel.getElement().getClientWidth();
		int paneHeight = _scrollPanel.getElement().getClientHeight();

		int viewportWidth = Math.max(paneWidth, bounds.w + 100);
		int viewportHeight = Math.max(paneHeight, bounds.h + 100);

		_viewportPanel.setPixelSize(viewportWidth, viewportHeight);
		_arrowsWidget.setCanvasSize(viewportWidth, viewportHeight);

		// reset widget positions
		setBranchPositions(_rootNode, -bounds.x + (viewportWidth - bounds.w)
				/ 2, -bounds.y + (viewportHeight - bounds.h) / 2);

		redraw();

		// XXX why is this in deferred command?
		DeferredCommand.addCommand(new Command() {

			@Override
			public void execute() {
				if (_actionsPanelView != null) {
					_actionsPanel.showNextTo(_actionsPanelView);
				}

			}
		});

	}

	private void setBranchPositions(NodeWidget node, int x, int y) {
		// TODO optimize by not updating every widget
		_viewportPanel.setWidgetPosition(node, x, y);
		for (LayoutTreeElement child : node.getLayoutChildren()) {
			if (child instanceof NodeWidget) {
				setBranchPositions((NodeWidget) child, x + child.getOffsetX(),
						y + child.getOffsetY());
			}
		}
	}

	@Override
	public void onNodeLayoutInvalidated(NodeWidget node) {
		updateLayout();
	}
}
