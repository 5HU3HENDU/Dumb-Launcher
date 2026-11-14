package com.dumb.launcher;

import android.graphics.Color;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * All category data for Dumb Launcher: names, tile gradients, legend dot colors,
 * and the package-name to category seed map. Pure data, no logic beyond lookup.
 * Category names, order and colors match the Dumb Launcher webpage exactly.
 * Apps not in the seed map land in OTHER; edit this file to recategorize an app.
 */
public final class CategoryMap {

    public static final int SOCIAL = 0;
    public static final int CHAT = 1;
    public static final int MONEY = 2;
    public static final int SHOPPING = 3;
    public static final int FOOD = 4;
    public static final int TRAVEL = 5;
    public static final int VIDEO = 6;
    public static final int MUSIC = 7;
    public static final int WORK = 8;
    public static final int UTILITIES = 9;
    public static final int OTHER = 10;

    public static final int COUNT = 11;

    public static final String[] NAMES = {
            "Social", "Chat", "Money", "Shopping", "Food & Drink",
            "Travel & Maps", "Video", "Music & Audio", "Work & Files",
            "Utilities", "Other"
    };

    /** Legend dot colors, from the webpage legend. */
    private static final String[] DOT_HEX = {
            "#e64980", "#12b76a", "#0e9f6e", "#f59f00", "#e8590c",
            "#1c7ed6", "#e03131", "#7048e8", "#4263eb", "#868e96", "#6b7280"
    };

    /** Tile gradient pairs (top-left, bottom-right), from the webpage tiles. */
    private static final int[][] GRADIENTS = {
            {Color.rgb(255, 99, 154), Color.rgb(208, 51, 106)},
            {Color.rgb(44, 209, 132), Color.rgb(0, 161, 84)},
            {Color.rgb(40, 185, 136), Color.rgb(0, 137, 88)},
            {Color.rgb(255, 185, 26), Color.rgb(223, 137, 0)},
            {Color.rgb(255, 115, 38), Color.rgb(210, 67, 0)},
            {Color.rgb(54, 152, 240), Color.rgb(6, 104, 192)},
            {Color.rgb(250, 75, 75), Color.rgb(202, 27, 27)},
            {Color.rgb(138, 98, 255), Color.rgb(90, 50, 210)},
            {Color.rgb(92, 125, 255), Color.rgb(44, 77, 213)},
            {Color.rgb(160, 168, 176), Color.rgb(112, 120, 128)},
            {Color.rgb(120, 128, 136), Color.rgb(80, 86, 92)},
    };

    public static int dotColor(int category) {
        return Color.parseColor(DOT_HEX[category]);
    }

    public static int[] gradient(int category) {
        return GRADIENTS[category];
    }

    private static final Map<String, Integer> MAP = new HashMap<>();

    static {
        // Social
        put(SOCIAL, "com.facebook.katana", "com.instagram.android", "com.linkedin.android",
                "org.joinmastodon.android", "com.pinterest", "com.quora.android",
                "com.reddit.frontpage", "com.snapchat.android", "com.instagram.barcelona",
                "com.twitter.android", "com.x.android", "com.facebook.lite",
                "org.joinmastodon.android.sk", "app.pachli", "com.tumblr");
        // Chat
        put(CHAT, "com.discord", "jp.naver.line.android", "com.google.android.apps.messaging",
                "com.facebook.orca", "org.thoughtcrime.securesms", "com.slack",
                "org.telegram.messenger", "com.viber.voip", "com.tencent.mm",
                "com.whatsapp", "com.whatsapp.w4b", "org.telegram.messenger.web",
                "com.microsoft.teams", "com.google.android.apps.tachyon");
        // Money
        put(MONEY, "com.dreamplug.androidapp", "com.google.android.apps.nbu.paisa.user",
                "com.nextbillion.groww", "com.snapwork.hdfc", "com.paypal.android.p2pmobile",
                "net.one97.paytm", "com.phonepe.app", "com.sbi.lotusintouch",
                "com.wise.android", "com.zerodha.kite3", "com.icicibank.imobile",
                "com.axis.mobile", "com.sbi.SBIFreedomPlus", "com.kotak.bank",
                "in.org.npci.upiapp", "com.bhimupi.app", "com.mobikwik_new",
                "com.freecharge.android", "com.angelbroking.angelone", "com.upstox.pro",
                "in.co.5paisa", "com.msf.angelmobile", "com.coinswitch.kuber",
                "io.bluecrew.pay", "com.etrade.mobilepro.activity");
        // Shopping
        put(SHOPPING, "com.ril.ajio", "com.amazon.mShop.android.shopping", "com.bigbasket.mobileapp",
                "com.grofers.customerapp", "com.ebay.mobile", "com.flipkart.android",
                "ingka.ikea.app", "com.meesho.supply", "com.myntra.android",
                "com.nykaa", "com.snapdeal.main", "com.shopclues",
                "in.amazon.mShop.android.shopping", "com.tatacliq", "com.croma.android",
                "com.reliancedigital.in", "com.jiomart.sellerapp", "com.myjio.jioMart",
                "com.dmart.ready", "com.zepto", "com.swiggy.instamart");
        // Food & Drink
        put(FOOD, "in.burgerking.android", "com.Dominos", "com.faasos",
                "com.freshmenu", "com.licious", "com.mcdonalds.app",
                "com.pizzahut.phl", "com.starbucks.mobilecard", "in.swiggy.android",
                "com.application.zomato", "com.starbucks.in", "com.mcd.android",
                "com.eatsure", "com.box8.consumer", "com.ovenstory.customer");
        // Travel & Maps
        put(TRAVEL, "com.airbnb.android", "com.booking", "com.google.android.apps.maps",
                "cris.org.in.prs.ima", "com.ixigo", "com.ixigo.trains",
                "com.makemytrip", "com.olacabs.customer", "com.rapido.passenger",
                "in.redbus.android", "com.ubercab", "com.goibibo",
                "com.yatra.base", "com.trivago", "com.agoda.mobile.consumer",
                "com.google.earth", "com.nrapps", "com.rapido.captain");
        // Video
        put(VIDEO, "com.apple.atve.sony.appletv", "in.startv.hotstar", "com.mxtech.videoplayer.ad",
                "com.netflix.mediaclient", "com.amazon.avod.thirdpartyclient", "com.sonyliv",
                "tv.twitch.android.app", "com.vimeo.android.videoapp", "com.google.android.youtube",
                "com.graymatrix.did", "in.startv.hotstar.dplus", "com.mxtech.videoplayer.pro",
                "com.jio.jioplay.tv", "com.viu.pad", "com.viki.android");
        // Music & Audio
        put(MUSIC, "com.amazon.mp3", "com.apple.android.music", "com.audible.application",
                "com.gaana", "com.hungama.myplay.activity", "com.jio.media.jiobeats",
                "com.shazam.android", "com.soundcloud.android", "com.spotify.music",
                "com.google.android.apps.youtube.music", "com.amazon.music",
                "com.wynk.music", "com.pandora.android", "deezer.android.app");
        // Work & Files
        put(WORK, "com.google.android.calendar", "com.google.android.apps.docs.editors.docs",
                "com.dropbox.android", "com.google.android.gm", "com.google.android.apps.docs",
                "com.google.android.apps.meetings", "notion.id", "com.microsoft.skydrive",
                "com.google.android.apps.docs.editors.sheets", "us.zoom.videomeetings",
                "com.microsoft.office.outlook", "com.google.android.apps.docs.editors.slides",
                "com.evernote", "com.todoist", "com.google.android.apps.tasks",
                "com.asana.app", "com.trello", "com.slack.workspaces", "com.microsoft.office.word",
                "com.microsoft.office.excel", "com.microsoft.office.powerpoint",
                "com.adobe.reader", "com.google.android.apps.authenticator2",
                "com.microsoft.authenticator", "com.lastpass.lpandroid");
        // Utilities
        put(UTILITIES, "com.google.android.calculator", "com.google.android.GoogleCamera",
                "com.android.chrome", "com.google.android.deskclock", "com.google.android.documentsui",
                "com.google.android.apps.nbu.files", "com.google.android.keep",
                "com.google.android.apps.photos", "com.android.settings",
                "com.google.android.contacts", "com.google.android.dialer",
                "com.android.vending", "com.google.android.apps.mapslite",
                "com.miui.securitycenter", "com.google.android.apps.wellbeing",
                "com.opera.browser", "org.mozilla.firefox", "com.brave.browser",
                "com.microsoft.emmx", "com.sec.android.app.camera", "com.android.camera2",
                "com.google.android.apps.translate", "com.google.android.apps.subscriptions.red");
    }

    private static void put(int category, String... packages) {
        for (String p : packages) {
            MAP.put(p, category);
        }
    }

    /** Category for a package name; OTHER when unmapped. Case-insensitive. */
    public static int categoryFor(String packageName) {
        Integer c = MAP.get(packageName);
        if (c == null) {
            c = MAP.get(packageName.toLowerCase(Locale.ROOT));
        }
        return c != null ? c : OTHER;
    }

    private CategoryMap() {}
}
