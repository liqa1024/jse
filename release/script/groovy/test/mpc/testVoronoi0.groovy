package test.mpc

import jse.atom.XYZ
import jsex.voronoi.VoronoiBuilder

import static jse.code.UT.Math.*
import static jse.code.UT.Plot.*


vbuild = new VoronoiBuilder().setNoWarning();

for (i in 0..<40) vbuild.insert(new XYZ(rand(), rand(), 0.0));

allVertex = vbuild.allVertex();
allTet = vbuild.allTetrahedron();

axis(0, 1, 0, 1)

plot((allVertex*.x()), (allVertex*.y()), 'vertex').lineType('none').marker('o').color(0).filled();

for (vertex in allVertex) for (neighbor in vertex.neighborVertex()) {
    plot([vertex.x(), neighbor.x()], [vertex.y(), neighbor.y()]).noLegend().color(0);
}

plot((allTet*.centerSphere()*.x()), (allTet*.centerSphere()*.y()), 'voronoi').lineType('none').marker('o').color(1);

for (tet in allTet) for (neighbor in tet.neighborTetrahedron()) {
    // 手动截断非法线
    if (hypot(tet.centerSphere().x()-neighbor.centerSphere().x(), tet.centerSphere().y()-neighbor.centerSphere().y()) > 10.0) continue;
    plot([tet.centerSphere().x(), neighbor.centerSphere().x()], [tet.centerSphere().y(), neighbor.centerSphere().y()]).noLegend().color(1);
}
