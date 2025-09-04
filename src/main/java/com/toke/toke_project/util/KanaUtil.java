package com.toke.toke_project.util;

public class KanaUtil {
	private static final String[] GROUPS = { "あ", "か", "さ", "た", "な", "は", "ま", "や", "ら", "わ" };
	private static final char[][] GROUP_HEADS = { { 'あ', 'い', 'う', 'え', 'お' },
			{ 'か', 'き', 'く', 'け', 'こ', 'が', 'ぎ', 'ぐ', 'げ', 'ご' }, { 'さ', 'し', 'す', 'せ', 'そ', 'ざ', 'じ', 'ず', 'ぜ', 'ぞ' },
			{ 'た', 'ち', 'つ', 'て', 'と', 'だ', 'ぢ', 'づ', 'で', 'ど' }, { 'な', 'に', 'ぬ', 'ね', 'の' },
			{ 'は', 'ひ', 'ふ', 'へ', 'ほ', 'ば', 'び', 'ぶ', 'べ', 'ぼ', 'ぱ', 'ぴ', 'ぷ', 'ぺ', 'ぽ' }, { 'ま', 'み', 'む', 'め', 'も' },
			{ 'や', 'ゆ', 'よ' }, { 'ら', 'り', 'る', 'れ', 'ろ' }, { 'わ', 'を', 'ん' } };

	public static String groupOf(String kana) {
		if (kana == null || kana.isBlank())
			return null;
		char c = kana.trim().charAt(0);
		for (int i = 0; i < GROUP_HEADS.length; i++) {
			for (char h : GROUP_HEADS[i]) {
				if (c == h)
					return GROUPS[i];
			}
		}

		return "#";
	}

	public static int vowelIndexOf(String kana) {
		if (kana == null || kana.isBlank())
			return 99;
		char c = kana.trim().charAt(0);
		String vowels = "あいうえおアイウエオ";
		int idx = vowels.indexOf(c);
		return (idx >= 0) ? (idx % 5) : 99;
	}

}
