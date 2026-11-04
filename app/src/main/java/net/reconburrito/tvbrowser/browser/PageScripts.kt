package net.reconburrito.tvbrowser.browser

/**
 * Small JavaScript helpers evaluated in the page. All take viewport (CSS px) coordinates.
 * They never run page-controlled code paths beyond standard DOM queries.
 */
object PageScripts {

    /** Returns the href of the closest anchor under (x,y) or "" */
    fun linkAt(x: Float, y: Float): String = """
        (function(){try{
          var e=document.elementFromPoint($x,$y); if(!e) return "";
          var a=e.closest ? e.closest('a[href]') : null;
          return a ? a.href : "";
        }catch(err){return "";}})();
    """.trimIndent()

    /**
     * Finds the nearest clickable element in direction (dx,dy) from (x,y) within the viewport and
     * returns "cx,cy" of its center, or "" when nothing qualifies.
     */
    fun snapTarget(x: Float, y: Float, dx: Int, dy: Int): String = """
        (function(){try{
          var sel='a[href],button,input:not([type=hidden]),select,textarea,[role=button],[role=link],[onclick],[tabindex]:not([tabindex="-1"]),video,summary,label';
          var els=document.querySelectorAll(sel);
          var vw=window.innerWidth, vh=window.innerHeight;
          var best=null, bestScore=1e12;
          for(var i=0;i<els.length;i++){
            var r=els[i].getBoundingClientRect();
            if(r.width<4||r.height<4) continue;
            if(r.right<0||r.bottom<0||r.left>vw||r.top>vh) continue;
            var cx=r.left+r.width/2, cy=r.top+r.height/2;
            var ex=cx-($x), ey=cy-($y);
            var along=ex*($dx)+ey*($dy);
            if(along<8) continue;
            var across=Math.abs(ex*($dy))+Math.abs(ey*($dx));
            if(across>along*1.2+60) continue;
            var score=along+across*2.5;
            if(score<bestScore){bestScore=score;best=[cx,cy];}
          }
          return best?best[0]+","+best[1]:"";
        }catch(err){return "";}})();
    """.trimIndent()

    /** Toggle play/pause on the first <video> (used for the remote's play/pause key). */
    const val TOGGLE_VIDEO = """
        (function(){try{
          var v=document.querySelector('video'); if(!v) return "none";
          if(v.paused){v.play();return "play";} else {v.pause();return "pause";}
        }catch(err){return "err";}})();
    """

    fun unquote(jsResult: String?): String {
        if (jsResult == null || jsResult == "null") return ""
        var s = jsResult
        if (s.length >= 2 && s.startsWith("\"") && s.endsWith("\"")) s = s.substring(1, s.length - 1)
        return s.replace("\\\"", "\"").replace("\\/", "/")
    }
}
