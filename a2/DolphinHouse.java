package a2;

import tage.shapes.ManualObject;

public class DolphinHouse extends ManualObject {

    float[] vertices = new float[]{
            -1f, -1f, -1f, 1f, -1f, -1f, 1f, -1f, 1f, // floor top right
            -1f, -1f, -1f, 1f, -1f, 1f, -1f, -1f, 1f, // floor bottom left
            -1f, -1f, -1f, 1f, -1f, -1f, 1f, 1f, -1f, // back wall bottom right
            -1f, -1f, -1f, -1f, 1f, -1f, 1f, 1f, -1f, // back wall top left
            1f, -1f, -1f, 1f, -1f, 1f, 1f, 1f, 1f,    // right wall bottom right
            1f, -1f, -1f, 1f, 1f, -1f, 1f, 1f, 1f,    // right wall top left
            -1f, -1f, 1f, 1f, -1f, 1f, 1f, 1f, 1f,    // front wall bottom right
            -1f, -1f, 1f, -1f, 1f, 1f, 1f, 1f, 1f,    // front wall top left

            -1f, 1f, -1f, 1f, 1f, -1f, 0f, 2f, 0f,    // back roof
            1f, 1f, -1f, 1f, 1f, 1f, 0f, 2f, 0f,      // right roof
            1f, 1f, 1f, -1f, 1f, 1f, 0f, 2f, 0f,      // front roof
            -1f, 1f, -1f, -1f, 1f, 1f, 0f, 2f, 0f,    // left roof

    };

    float[] texCoords = {
            0f, 0f, 0f, 1f, 1f, 1f,    // bottom floor
            0f, 0f, 1f, 1f, 1f, 0f,
            0f, 0f, 1f, 0f, 1f, 1f,    // back wall
            0f, 0f, 0f, 1f, 1f, 1f,
            0f, 0f, 1f, 0f, 1f, 1f,    // right wall
            0f, 0f, 0f, 1f, 1f, 1f,
            0f, 0f, 1f, 0f, 1f, 1f,    // front wall
            0f, 0f, 0f, 1f, 1f, 1f,
            0f, 0f, 1f, 0f, 0.5f,0.5f, //roof
            0f, 0f, 1f, 0f, 0.5f,0.5f,
            0f, 0f, 1f, 0f, 0.5f,0.5f,
            0f, 0f, 1f, 0f, 0.5f,0.5f,
    };

    float[] normals = {
            0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f,    // floor (pointing DOWN)
            0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f,
            0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f,    // back wall (pointing BACK)
            0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f,
            1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f,       // right wall (pointing RIGHT)
            1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f,       // front wall (pointing FORWARD)
            0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f,

            // Roof normals angled outward and upward
            0f, 1f, -1f, 0f, 1f, -1f, 0f, 1f, -1f,    // back roof
            1f, 1f, 0f, 1f, 1f, 0f, 1f, 1f, 0f,       // right roof
            0f, 1f, 1f, 0f, 1f, 1f, 0f, 1f, 1f,       // front roof
            -1f, 1f, 0f, -1f, 1f, 0f, -1f, 1f, 0f     // left roof
    };

    public DolphinHouse() {
        super();
        setNumVertices(36);
        setVertices(vertices);
        setTexCoords(texCoords);
        setNormals(normals);
        setWindingOrderCCW(true);
    }
}
