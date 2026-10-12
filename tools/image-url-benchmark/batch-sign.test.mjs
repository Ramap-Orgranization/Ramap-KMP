import test from "node:test";
import assert from "node:assert/strict";
import { batchSign } from "./batch-sign.mjs";
const images = [{bucket:"profile-avatars",path:"a/b.jpg"},{bucket:"shop-review-images",path:"c/d.jpg"}];
const base = "https://example.supabase.co";
const success = (bucket,path) => ({path,error:null,signedURL:`/object/sign/${bucket}/${path}?token=fake-test-token`});
test("deduplicates paths, makes one call per bucket and uses caller credentials", async () => {
    const requests=[];
    const result = await batchSign([...images,images[0]],base,"anon","Bearer caller",async (url,options)=>{
        requests.push({url,options});
        assert.equal(options.headers.Authorization,"Bearer caller");
        assert.equal(options.headers.apikey,"anon");
        const body=JSON.parse(options.body); assert.equal(body.expiresIn,300);
        return Response.json(body.paths.map(p=>success(url.split("/").at(-1),p)));
    });
    assert.equal(requests.length,2); assert.equal(result.metrics.issued,2);
    assert.equal(result.metrics.requested,3); assert.equal(result.images.length,2);
});
test("counts HTTP200 per-image errors and missing results",async()=>{
    const result = await batchSign(images,base,"anon","Bearer caller",async()=>Response.json([]));
    assert.equal(result.metrics.failed,2); assert.equal(result.metrics.issued,0);
});
test("rejects mismatched signed paths",async()=>{
    const result=await batchSign(images,base,"anon","Bearer caller",async(url,options)=>{
        const path=JSON.parse(options.body).paths[0];
        return Response.json([{...success("wrong-bucket",path)}]);
    });
    assert.equal(result.metrics.failed,2);
});
test("rejects ambiguous duplicate results",async()=>{
    const result=await batchSign([images[0]],base,"anon","Bearer caller",async()=>Response.json([success(images[0].bucket,images[0].path),success(images[0].bucket,images[0].path)]));
    assert.equal(result.metrics.failed,1); assert.equal(result.metrics.issued,0);
});
test("counts HTTP failure for every affected image",async()=>{
    const result=await batchSign(images,base,"anon","Bearer caller",async()=>new Response(null,{status:503}));
    assert.equal(result.metrics.operations.sign.failures,2); assert.equal(result.metrics.failed,2);
});
test("records failures without leaking HTTP body or exceptions",async()=>{
    const result=await batchSign(images,base,"anon","Bearer caller",async()=>{throw new Error("secret")});
    assert.equal(result.metrics.operations.sign.requests,2); assert.equal(result.metrics.operations.sign.failures,2);
    assert.equal(result.metrics.failed,2); assert.ok(!JSON.stringify(result).includes("secret"));
});
