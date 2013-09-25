package cc.setting.piece;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;

import cc.core.ChineseCharacterTzu;
import cc.core.ChineseCharacterWen;
import cc.moveable_type.ChineseCharacterMovableTypeTzu;
import cc.moveable_type.piece.PieceMovableType;
import cc.moveable_type.piece.PieceMovableTypeTzu;
import cc.moveable_type.piece.PieceMovableTypeWen;
import cc.moveable_type.rectangular_area.分離活字;
import cc.moveable_type.rectangular_area.平面幾何;
import cc.moveable_type.rectangular_area.活字單元;

/**
 * 物件活字設定工具。將部件結構（<code>ChineseCharacter</code>）轉換成活字結構（
 * <code>PieceMovableType</code>）。把活字的資訊全部集中在同一個物件上（<code>Piece</code>，
 * <code>RectangularArea</code>型態 ），方便函式傳遞與使用，而且物件上也有相對應操縱的函式。
 * <p>
 * <code>SimplePiece</code>是在設定時兩兩配對後定框，調整時更改部件大小，但無法物件難實作距離貼近或拉開。
 * 
 * @author Ihc
 */
public class SimplePieceSetter extends 物件活字基礎設定工具
{
	/** 活字字型的名稱 */
	private String fontName;
	/** 活字字型的選項 */
	private int fontStyle;
	/** 活字的點距 */
	private int fontResolution;
	/** 活字的渲染屬性 */
	protected FontRenderContext fontRenderContext;
	/** 活字的字體 */
	protected Font font;

	/**
	 * 建立物件活字設定工具
	 * 
	 * @param fontName
	 *            活字字型的名稱
	 * @param fontStyle
	 *            活字字型的選項
	 * @param fontResolution
	 *            活字的點距
	 * @param fontRenderContext
	 *            活字的渲染屬性
	 */
	public SimplePieceSetter(String fontName, int fontStyle,
			int fontResolution, FontRenderContext fontRenderContext)
	{
		super(null, null);
		this.fontName = fontName;
		this.fontStyle = fontStyle;
		this.fontResolution = fontResolution;
		this.fontRenderContext = fontRenderContext;
		this.font = new Font(fontName, fontStyle, fontResolution);
		GlyphVector glyphVector = font.createGlyphVector(fontRenderContext,
				SimplePieceSetter.tzuModelCharacter);
		this.tzuModelTerritory = glyphVector.getOutline().getBounds2D();
		BasicStroke basicStroke = new BasicStroke();
		this.pieceForNoBuiltInWen = new Area(
				basicStroke.createStrokedShape(tzuModelTerritory));
	}

	@Override
	public PieceMovableTypeWen setWen(ChineseCharacterMovableTypeTzu parent,
			ChineseCharacterWen chineseCharacterWen)
	{
		活字單元 rectangularArea = null;
		if (font.canDisplay(chineseCharacterWen.getCodePoint()))
		{
			GlyphVector glyphVector = font.createGlyphVector(fontRenderContext,
					chineseCharacterWen.getChars());
			rectangularArea = new 分離活字(new 平面幾何(glyphVector.getOutline()));
//			rectangularArea.設字範圍(tzuModelTerritory);
			rectangularArea.設目標範圍(rectangularArea.字範圍());
		}
		else
		{
			rectangularArea = findWenForNoBuiltIn(chineseCharacterWen);
		}
		rectangularArea.徙轉原點();

		PieceMovableTypeWen pieceMovableTypeWen = new PieceMovableTypeWen(
				parent, chineseCharacterWen, rectangularArea);
		return pieceMovableTypeWen;
	}

	@Override
	public PieceMovableTypeTzu setTzu(ChineseCharacterMovableTypeTzu parent,
			ChineseCharacterTzu chineseCharacterTzu)
	{
		PieceMovableTypeTzu pieceMovableTypeTzu = new PieceMovableTypeTzu(
				parent, chineseCharacterTzu, new 分離活字(new 平面幾何()));

		setChildrenRecursively(pieceMovableTypeTzu, chineseCharacterTzu);

		switch (chineseCharacterTzu.getType())
		{
		case horizontal:
			horizontalSetting(pieceMovableTypeTzu);
			break;
		case vertical:
			verticalSetting(pieceMovableTypeTzu);
			break;
		case wrap:
			wrapSetting(pieceMovableTypeTzu);
			break;
		case 注音符號:// TODO 看情形才決定欲修改無，先用垂直的
			horizontalSetting(pieceMovableTypeTzu);
			break;
		case 異寫字編號符號:
			System.out.println("無事先共異寫字換掉");
			break;
		}

		if (pieceMovableTypeTzu.getParent() == null)
			pieceMovableTypeTzu.getPiece().設目標範圍(tzuModelTerritory);

		return pieceMovableTypeTzu;
	}

	/**
	 * 設定底下活字部件
	 * 
	 * @param chineseCharacterMovableTypeTzu
	 *            目前設定的合體活字
	 * @param chineseCharacterTzu
	 *            目前設定的字部件
	 */
	protected void setChildrenRecursively(
			ChineseCharacterMovableTypeTzu chineseCharacterMovableTypeTzu,
			ChineseCharacterTzu chineseCharacterTzu)
	{
		for (int i = 0; i < chineseCharacterMovableTypeTzu.getChildren().length; ++i)
		{
			chineseCharacterMovableTypeTzu.getChildren()[i] = chineseCharacterTzu
					.getChildren()[i].typeset(this,
					chineseCharacterMovableTypeTzu);
		}
		return;
	}

	/**
	 * 水平組合活字
	 * 
	 * @param pieceMovableTypeTzu
	 *            要設定的合體活字
	 */
	protected void horizontalSetting(PieceMovableTypeTzu pieceMovableTypeTzu)
	{
		PieceMovableType firstChild = (PieceMovableType) pieceMovableTypeTzu
				.getChildren()[0], secondChild = (PieceMovableType) pieceMovableTypeTzu
				.getChildren()[1];
		Rectangle2D.Double rectDouble = new Rectangle2D.Double();
		rectDouble.width = firstChild.getPiece().目標範圍().getWidth()
				+ secondChild.getPiece().目標範圍().getWidth();
		rectDouble.height = Math.max(firstChild.getPiece().目標範圍().getHeight(),
				secondChild.getPiece().目標範圍().getHeight());
		pieceMovableTypeTzu.getPiece().設目標範圍大細(rectDouble.width,
				rectDouble.height);
		pieceMovableTypeTzu.getPiece().合併活字(
				new 分離活字(new 平面幾何(new Area(rectDouble))));
		firstChild
				.getPiece()
				.目標範圍()
				.setRect(0.0, 0.0, firstChild.getPiece().目標範圍().getWidth(),
						rectDouble.height);
		secondChild
				.getPiece()
				.目標範圍()
				.setRect(firstChild.getPiece().目標範圍().getWidth(), 0.0,
						secondChild.getPiece().目標範圍().getWidth(),
						rectDouble.height);
		return;

	}

	/**
	 * 垂直組合活字
	 * 
	 * @param pieceMovableTypeTzu
	 *            要設定的合體活字
	 */
	protected void verticalSetting(PieceMovableTypeTzu pieceMovableTypeTzu)
	{
		PieceMovableType firstChild = (PieceMovableType) pieceMovableTypeTzu
				.getChildren()[0], secondChild = (PieceMovableType) pieceMovableTypeTzu
				.getChildren()[1];
		Rectangle2D.Double rectDouble = new Rectangle2D.Double();
		rectDouble.width = Math.max(firstChild.getPiece().目標範圍().getWidth(),
				secondChild.getPiece().目標範圍().getWidth());
		rectDouble.height = firstChild.getPiece().目標範圍().getHeight()
				+ secondChild.getPiece().目標範圍().getHeight();
		pieceMovableTypeTzu.getPiece().設目標範圍大細(rectDouble.width,
				rectDouble.height);
		pieceMovableTypeTzu.getPiece().合併活字(
				new 分離活字(new 平面幾何(new Area(rectDouble))));
		firstChild
				.getPiece()
				.目標範圍()
				.setRect(0.0, 0.0, rectDouble.width,
						firstChild.getPiece().目標範圍().getHeight());
		secondChild
				.getPiece()
				.目標範圍()
				.setRect(0.0, firstChild.getPiece().目標範圍().getHeight(),
						rectDouble.width,
						secondChild.getPiece().目標範圍().getHeight());
		return;
	}

	/**
	 * 包圍組合活字
	 * 
	 * @param pieceMovableTypeTzu
	 *            要設定的合體活字
	 */
	protected void wrapSetting(PieceMovableTypeTzu pieceMovableTypeTzu)
	{
		// TODO 暫時替代用
		PieceMovableType firstChild = (PieceMovableType) pieceMovableTypeTzu
				.getChildren()[0], secondChild = (PieceMovableType) pieceMovableTypeTzu
				.getChildren()[1];
		Rectangle2D.Double rectDouble = new Rectangle2D.Double();
		rectDouble.width = firstChild.getPiece().目標範圍().getWidth() * 2;
		rectDouble.height = firstChild.getPiece().目標範圍().getHeight() * 2;
		pieceMovableTypeTzu.getPiece().設目標範圍大細(rectDouble.width,
				rectDouble.height);
		pieceMovableTypeTzu.getPiece().合併活字(
				new 分離活字(new 平面幾何(new Area(rectDouble))));
		firstChild.getPiece().目標範圍()
				.setRect(0.0, 0.0, rectDouble.width, rectDouble.height);
		secondChild
				.getPiece()
				.目標範圍()
				.setRect(
						(firstChild.getPiece().目標範圍().getWidth() - secondChild
								.getPiece().目標範圍().getWidth()) / 2,
						(firstChild.getPiece().目標範圍().getHeight() - secondChild
								.getPiece().目標範圍().getHeight()) / 2,
						secondChild.getPiece().目標範圍().getWidth(),
						secondChild.getPiece().目標範圍().getHeight());
		return;
	}

	/**
	 * 取得活字字型的名稱
	 * 
	 * @return 活字字型的名稱
	 */
	public String getFontName()
	{
		return fontName;
	}

	/**
	 * 取得活字字型的選項
	 * 
	 * @return 活字字型的選項
	 */
	public int getFontStyle()
	{
		return fontStyle;
	}

	/**
	 * 取得活字的點距
	 * 
	 * @return 活字的點距
	 */
	public int getFontResolution()
	{
		return fontResolution;
	}

	/**
	 * 取得活字的渲染屬性
	 * 
	 * @return 活字的渲染屬性
	 */
	public FontRenderContext getFontRenderContext()
	{
		return fontRenderContext;
	}
}
